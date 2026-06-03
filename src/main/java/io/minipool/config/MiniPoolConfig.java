package io.minipool.config;

public class MiniPoolConfig {

    private final String url;
    private final String username;
    private final String password;
    private final int minSize;
    private final int maxSize;
    private final long acquireTimeoutMs;

    public MiniPoolConfig(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
        this.minSize = builder.minSize;
        this.maxSize = builder.maxSize;
        this.acquireTimeoutMs = builder.acquireTimeoutMs;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public long getAcquireTimeoutMs() {
        return acquireTimeoutMs;
    }

    public static class Builder {

        private String url;
        private String username;
        private String password;
        private final int minSize = 2;
        private int maxSize = 10;
        private long acquireTimeoutMs = 3_000;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder maxSize(int maxSize) {
            if (maxSize < 1) {
                throw new IllegalArgumentException("maxSize must be greater than 0");
            }
            this.maxSize = maxSize;
            return this;
        }

        public Builder acquireTimeoutMs(long acquireTimeoutMs) {
            if (acquireTimeoutMs < 0) {
                throw new IllegalArgumentException("acquireTimeoutMs must be greater than 0");
            }
            this.acquireTimeoutMs = acquireTimeoutMs;
            return this;
        }

        public MiniPoolConfig build() {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("url cannot be null or empty");
            }

            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("username cannot be null or empty");
            }

            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("password cannot be null or empty");
            }

            return new MiniPoolConfig(this);
        }
    }
}
