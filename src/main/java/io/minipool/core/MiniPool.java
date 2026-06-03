package io.minipool.core;

import io.minipool.config.MiniPoolConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniPool implements AutoCloseable {

    private final MiniPoolConfig config;

    // stores idle connections waiting to be borrowed by the pool
    // BlockingQueue is used to ensure thread safety as poll() -> give a connection from the pool to a thread
    // and offer() -> return a connection to the pool operations are atomic.

    // Atomicity is ensured by internally by the BlockingQueue implementation by using locking.

    // Locking does the following:
    // Thread A locks queue
    // Thread A gets C1
    // Thread A unlocks
    //
    // Thread B locks queue
    // Thread B gets C2
    // Thread B unlocks
    //
    // Atomicity is crucial for managing concurrent access to the pool.
    //
    // But why? Suppose idle contains connections [C1, C2, C3]
    // If poll() were not atomic and two threads execute Connection conn = idle.poll(); at the same time
    // Thread A reads C1
    // Thread B reads C1
    // Thread A removes and receives C1
    // Thread B also removes and receives C1
    //
    // Now both threads are using the same connection C1, which is unsafe.
    // BlockingQueue prevents this.
    private final BlockingQueue<Connection> idle;

    // The reason why we use an AtomicInteger is the same as using the BlockingQueue,
    private final AtomicInteger totalCount = new AtomicInteger(0); // total number of connections opened

    private volatile boolean shutdown = false; // volatile = thread safety. changes made by one thread are immediately visible to other threads.

    public MiniPool(MiniPoolConfig config) throws SQLException {
        this.config = config;
        this.idle = new LinkedBlockingQueue<>(config.getMaxSize());

        for (int i = 0; i < config.getMinSize(); i++) {
            idle.add(openConnection());
            totalCount.incrementAndGet();
        }
    }

    /**
     * Acquires a connection from the pool, blocking if necessary until one becomes available.
     * 1) If an idle connection is available, return it.
     * 2) If the pool is below maxSize, open a new connection and return it.
     * 3) Otherwise, block until a connection becomes available or timeout is reached.
     *
     * @return a valid MiniPoolConnection
     * @throws SQLException         if a connection cannot be acquired within the timeout
     * @throws InterruptedException if the calling thread is interrupted while waiting for a connection
     */
    public MiniPoolConnection acquire() throws SQLException, InterruptedException {
        if (shutdown) {
            throw new IllegalStateException("MiniPool is shutdown");
        }

        // step 1
        Connection conn = idle.poll();
        if (conn != null) {
            return validated(conn);
        }

        // step 2
        int current;

        do {
            current = totalCount.get();
            if (current >= config.getMaxSize()) break;
        } while (!totalCount.compareAndSet(current, current + 1));

        if (current < config.getMaxSize()) {
            try {
                return lease(openConnection());
            } catch (SQLException e) {
                totalCount.decrementAndGet();
                throw e;
            }
        }

        // step 3
        conn = idle.poll(config.getAcquireTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (conn == null) {
            throw new SQLException(
                    "Timeout: no connection available after " + config.getAcquireTimeoutMs() + "ms " +
                            "(max size: " + config.getMaxSize() + ")"
            );
        }
        return validated(conn);
    }

    /**
     * Returns a borrowed connection to the pool.
     * Called automatically by MiniPoolConnection.close().
     */
    void release(Connection conn) {
        if (shutdown) {
            destroy(conn);
            totalCount.decrementAndGet();
            return;
        }
        try {
            resetForReuse(conn);
            if (!idle.offer(conn)) {
                // Queue is full so we discard the connection
                destroy(conn);
                totalCount.decrementAndGet();
            }
        } catch (SQLException e) {
            destroy(conn);
            totalCount.decrementAndGet();
        }
    }

    @Override
    public void close() {
        shutdown = true;
        Connection conn;
        while ((conn = idle.poll()) != null) {
            destroy(conn);
            totalCount.decrementAndGet();
        }
    }

    /**
     * Number of connections currently idle in the pool.
     */
    public int idleCount() {
        return idle.size();
    }

    /**
     * Total connections opened (idle + currently borrowed).
     */
    public int totalCount() {
        return totalCount.get();
    }

    /**
     * Opens a new physical JDBC connection to the database.
     */
    private Connection openConnection() throws SQLException {
        Connection raw = DriverManager.getConnection(
                config.getUrl(),
                config.getUsername(),
                config.getPassword()
        );
        raw.setAutoCommit(true);
        return raw;
    }

    private MiniPoolConnection lease(Connection conn) {
        return new MiniPoolConnection(conn, this);
    }

    /**
     * Validates a connection before handing it to a caller.
     * If stale (e.g., db restarted), discards it and opens a fresh one.
     */
    private MiniPoolConnection validated(Connection conn) throws SQLException {
        if (!conn.isValid(2)) {
            destroy(conn);
            try {
                return lease(openConnection());
            } catch (SQLException e) {
                totalCount.decrementAndGet();
                throw e;
            }
        }
        return lease(conn);
    }

    /**
     * Restores the physical connection before making it available to another borrower.
     */
    private void resetForReuse(Connection conn) throws SQLException {
        if (!conn.getAutoCommit()) {
            conn.rollback();
            conn.setAutoCommit(true);
        }
    }

    private void destroy(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ignored) {
            // connection is already gone or broken
        }
    }
}
