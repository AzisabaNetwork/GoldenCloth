package net.azisaba.goldencloth.config;

public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String scheme;
    private final String username;
    private final String password;
    private final boolean useSSL;

    public DatabaseConfig(String host, int port, String scheme, String username, String password, boolean useSSL) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUseSSL() {
        return useSSL;
    }
}
