package io.minipool.core;

import java.sql.Connection;

/**
 * A borrowed connection from the pool.
 */
public class MiniPoolConnection implements AutoCloseable {

    private final Connection connection;
    private final MiniPool minipool;
    private boolean returned = false;

    public MiniPoolConnection(Connection connection, MiniPool minipool) {
        this.connection = connection;
        this.minipool = minipool;
    }

    // Returns the JDBC connection.
    public Connection getConnection() {
        if (returned) {
            throw new IllegalStateException("Connection already returned");
        }
        return this.connection;
    }

    // Returns the connection to the pool.
    @Override
    public void close() {
        if (!returned) {
            returned = true;
            minipool.release(connection);
        }
    }
}
