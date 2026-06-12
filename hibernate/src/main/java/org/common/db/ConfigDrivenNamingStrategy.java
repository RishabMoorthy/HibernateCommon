package org.common.db;

import org.common.db.config.ConfigLoader;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class ConfigDrivenNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment env) {

        String configKey = resolveConfigKey(logicalName.getText());

        if (configKey != null) {
            String tableName = ConfigLoader.getProperty(configKey);
            if (tableName != null && !tableName.isBlank()) {
                System.out.println("[NamingStrategy] " + logicalName.getText() + " -> " + tableName);
                return env.getIdentifierHelper().toIdentifier(tableName, false);
            }
        }
        // fallback — use logical name as-is
        return logicalName;
    }

    private String resolveConfigKey(String entityName) {
        switch (entityName) {
            case "VSDetails":           return "stubserver.vstable";
            case "VsCatalog":           return "stubserver.vscatalogtable";
            case "VsExecutionMode":     return "stubserver.executionmode";
            case "VsLiveUrl":           return "stubserver.liveurl";
            case "DailyMetrics":        return "stubserver.metrics";
            case "MasterCatalog":       return "stubserver.mastercatalogtable";
            default:                    return null;
        }
    }

    @Override
    public Identifier toPhysicalCatalogName(Identifier i, JdbcEnvironment e) { return i; }

    @Override
    public Identifier toPhysicalSchemaName(Identifier i, JdbcEnvironment e) { return i; }

    @Override
    public Identifier toPhysicalColumnName(Identifier i, JdbcEnvironment e) { return i; }

    @Override
    public Identifier toPhysicalSequenceName(Identifier i, JdbcEnvironment e) { return i; }
}
