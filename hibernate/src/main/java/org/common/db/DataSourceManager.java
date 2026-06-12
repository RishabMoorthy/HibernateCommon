package org.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.common.db.config.ConfigLoader;
import org.common.db.config.EncryptDecrypt;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * - Single HikariCP pool initialized from config at startup.
 * - DB type is auto-detected from database.url — no hardcoding.
 *
 * - config_uat.properties / config_local.properties drives everything:
 * - database.url      -> detects Oracle / Derby / MySQL / Postgres
 * - database.user     -> pool username
 * - database.password -> decrypted by EncryptDecrypt
 */
public class DataSourceManager {
    private static final DataSourceManager instance = new DataSourceManager();

    private final HikariDataSource dataSource;
    private final DatabaseType     databaseType;

    private DataSourceManager() {
        String jdbcUrl = ConfigLoader.getProperty("database.url");

        // Auto-detect DB type from the URL prefix
        this.databaseType = DatabaseType.fromUrl(jdbcUrl);

        System.out.println("[DataSourceManager] Detected DB: " + databaseType);

        this.dataSource = initPool(jdbcUrl);

        System.out.println("[DataSourceManager] Pool initialized for " + databaseType);
    }

    public static DataSourceManager getInstance() {
        return instance;
    }

    // Pool init
    private HikariDataSource initPool(String jdbcUrl) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName(databaseType.getDriverClass());
        config.setConnectionTestQuery(databaseType.getTestQuery());
        config.setPoolName(databaseType.name() + "-Pool");

        // Only set user/password if defined (Derby embedded may not need them)
        String user = ConfigLoader.getProperty("database.user");
        String pass = ConfigLoader.getProperty("database.password");
        if (user != null && !user.isBlank()) {
            config.setUsername(user);
        }
        if (pass != null && !pass.isBlank()) {
            config.setPassword(EncryptDecrypt.decryptPassword(pass));
        }

        // Pool sizing — sensible defaults, override via config if needed
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(5);
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(1_800_000);
        config.setConnectionTimeout(5_000);

        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Failed to initialize " + databaseType + " connection pool", e);
        }
    }

    // Public API
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool is not initialised");
        }
        return dataSource.getConnection();
    }

    // Shutdown
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DataSourceManager] " + databaseType + " pool closed");
        }
    }
}
