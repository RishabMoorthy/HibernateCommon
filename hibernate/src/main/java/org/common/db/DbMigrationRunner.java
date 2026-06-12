package org.common.db;

import org.flywaydb.core.Flyway;
import org.common.db.config.ConfigLoader;

import java.util.Map;

public class DbMigrationRunner {

    public static void migrateIfEnabled() {

        String enabled = ConfigLoader.getProperty("database.migration.enabled");

        if (!"true".equalsIgnoreCase(enabled)) {
            System.out.println("[DbMigrationRunner] DB migration disabled");
            return;
        }

        DatabaseType databaseType = DataSourceManager.getInstance().getDatabaseType();
        String migrationLocation = resolveMigrationLocation(databaseType);

        System.out.println("[DbMigrationRunner] Running DB migrations for "
                + databaseType + " from " + migrationLocation);

        Flyway flyway = Flyway.configure()
                .dataSource(DataSourceManager.getInstance().getDataSource())
                .locations(migrationLocation)
                .placeholders(Map.of(
                        "schema", ConfigLoader.getProperty("database.schema")
                ))
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();

        System.out.println("[DbMigrationRunner] DB migration completed");
    }

    private static String resolveMigrationLocation(DatabaseType databaseType) {
        switch (databaseType) {
            case MYSQL:     return "classpath:db/migration/mysql";
            case SQLSERVER: return "classpath:db/migration/sqlserver";
            case DERBY:     return "classpath:db/migration/derby";
            case ORACLE:    return "classpath:db/migration/oracle";
            case POSTGRES:  return "classpath:db/migration/postgres";
            default:
                throw new IllegalStateException(
                        "No migration path configured for DB type: " + databaseType);
        }
    }
}
