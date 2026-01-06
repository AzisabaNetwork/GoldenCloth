package net.azisaba.goldencloth.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.azisaba.goldencloth.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager implements AutoCloseable {
    private final DatabaseConfig config;
    private final Logger logger;
    private HikariDataSource dataSource;
    private GoldenClothRepository repository;

    public DatabaseManager(DatabaseConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void initialize() {
        HikariConfig hikariConfig = new HikariConfig();
        String jdbcUrl = "jdbc:mariadb://"
                + config.getHost()
                + ":"
                + config.getPort()
                + "/"
                + config.getScheme()
                + "?useSSL="
                + config.isUseSSL();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("GoldenClothPool");

        dataSource = new HikariDataSource(hikariConfig);
        repository = new GoldenClothRepository(dataSource);
        createTables();
    }

    public GoldenClothRepository getRepository() {
        return repository;
    }

    private void createTables() {
        String playersSql = "CREATE TABLE IF NOT EXISTS players ("
                + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,"
                + "uuid CHAR(36) NOT NULL,"
                + "name VARCHAR(16) NOT NULL,"
                + "name_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_players_uuid (uuid)"
                + ") ENGINE=InnoDB";

        String lotsSql = "CREATE TABLE IF NOT EXISTS golden_cloth_lots ("
                + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,"
                + "player_id BIGINT UNSIGNED NOT NULL,"
                + "purchased_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "expires_at TIMESTAMP NOT NULL,"
                + "amount INT NOT NULL,"
                + "spent INT NOT NULL DEFAULT 0,"
                + "balance_at_purchase INT NOT NULL,"
                + "note VARCHAR(255) NULL,"
                + "PRIMARY KEY (id),"
                + "KEY idx_gcl_player_expires (player_id, expires_at),"
                + "CONSTRAINT fk_gcl_player FOREIGN KEY (player_id) REFERENCES players(id)"
                + ") ENGINE=InnoDB";

        String transactionsSql = "CREATE TABLE IF NOT EXISTS golden_cloth_transactions ("
                + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,"
                + "player_id BIGINT UNSIGNED NOT NULL,"
                + "lot_id BIGINT UNSIGNED NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "amount_delta INT NOT NULL,"
                + "reason VARCHAR(64) NOT NULL,"
                + "reference VARCHAR(128) NULL,"
                + "balance_after INT NOT NULL,"
                + "PRIMARY KEY (id),"
                + "KEY idx_gct_player_created (player_id, created_at),"
                + "CONSTRAINT fk_gct_player FOREIGN KEY (player_id) REFERENCES players(id),"
                + "CONSTRAINT fk_gct_lot FOREIGN KEY (lot_id) REFERENCES golden_cloth_lots(id)"
                + ") ENGINE=InnoDB";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(playersSql);
            statement.execute(lotsSql);
            statement.execute(transactionsSql);
        } catch (SQLException e) {
            logger.severe("Failed to create tables: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
