# Minipool

Minipool is a small Java project for learning how a database connection pool
works.

It uses JDBC to open PostgreSQL connections, keeps a few of them around, and
reuses them instead of creating a new connection for every query.

The main idea is simple:

- `MiniPool` owns the real JDBC connections.
- `MiniPoolConnection` is the handle you borrow from the pool.
- Calling `close()` on `MiniPoolConnection` returns the connection to the pool.

## Run

Update the database settings in `src/main/java/io/minipool/App.java`, then run:

```bash
mvn compile
mvn exec:java
```
