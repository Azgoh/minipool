package io.minipool;

import io.minipool.config.MiniPoolConfig;
import io.minipool.core.MiniPool;
import io.minipool.core.MiniPoolConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class App {
    public static void main(String[] args) throws Exception {

        MiniPoolConfig config = new MiniPoolConfig.Builder()
                .url("jdbc:postgresql://localhost:5432/YOUR_DB_NAME")
                .username("YOUR_USERNAME")
                .password("YOUR_PASSWORD")
                .maxSize(10)
                .acquireTimeoutMs(3_000)
                .build();

        try (MiniPool pool = new MiniPool(config)) {
            System.out.println("Started idle=" + pool.idleCount() + " total=" + pool.totalCount());

            // create a table and insert some rows
            try (MiniPoolConnection conn = pool.acquire()) {
                try (PreparedStatement ps = conn.getConnection()
                        .prepareStatement("CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, name TEXT)")) {
                    ps.execute();
                }
            }

            try (MiniPoolConnection conn = pool.acquire()) {
                try (PreparedStatement ps = conn.getConnection()
                        .prepareStatement("INSERT INTO users (name) VALUES (?)")) {
                    ps.setString(1, "Bob");
                    ps.execute();
                }
            }

            try (MiniPoolConnection conn = pool.acquire()) {
                try (PreparedStatement ps = conn.getConnection()
                        .prepareStatement("SELECT * FROM users");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.println(rs.getInt("id") + " - " + rs.getString("name"));
                    }
                }
            }
            System.out.println("Released idle=" + pool.idleCount() + " total=" + pool.totalCount());
            System.out.println("Pool closed.");
        }


    }
}