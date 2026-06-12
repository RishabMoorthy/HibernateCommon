package com.stubserver.backend.database.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicTableNamingStrategy implements PhysicalNamingStrategy {

    private static final Map<String, String> TABLE_OVERRIDES = new ConcurrentHashMap<>();

    // Called from DatabaseConfig before Hibernate initializes
    public static void register(String logicalName, String physicalName) {
        TABLE_OVERRIDES.put(logicalName, physicalName);
    }

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        String resolved = TABLE_OVERRIDES.get(logicalName.getText());
        return resolved != null ? Identifier.toIdentifier(resolved) : logicalName;
    }

    @Override
    public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName;
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName;
    }
}
