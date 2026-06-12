package org.common.db;

public enum DatabaseType {
    ORACLE(
            "jdbc:oracle",
            "oracle.jdbc.driver.OracleDriver",
            "org.hibernate.dialect.OracleDialect",
            "SELECT 1 FROM DUAL"),
    DERBY(
            "jdbc:derby",
            "org.apache.derby.jdbc.EmbeddedDriver",
            "org.hibernate.dialect.DerbyDialect",
            "SELECT 1"),
    MYSQL(
            "jdbc:mysql",
            "com.mysql.cj.jdbc.Driver",
            "org.hibernate.dialect.MySQL8Dialect",
            "SELECT 1"),
    SQLSERVER(
            "jdbc:sqlserver",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "org.hibernate.dialect.SQLServerDialect",
            "SELECT 1"),
    POSTGRES(
            "jdbc:postgresql",
            "org.postgresql.Driver",
            "org.hibernate.dialect.PostgreSQLDialect",
            "SELECT 1");

    private final String urlPrefix;
    private final String driverClass;
    private final String hibernateDialect;
    private final String testQuery;

    DatabaseType(String urlPrefix, String driverClass, String hibernateDialect, String testQuery) {
        this.urlPrefix        = urlPrefix;
        this.driverClass      = driverClass;
        this.hibernateDialect = hibernateDialect;
        this.testQuery        = testQuery;
    }

    public String getDriverClass()      { return driverClass; }
    public String getHibernateDialect() { return hibernateDialect; }
    public String getTestQuery()        { return testQuery; }

    /**
     * Auto-detects DB type from the JDBC URL defined in config.
     * e.g. "jdbc:oracle:thin:@..." -> ORACLE
     */
    public static DatabaseType fromUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("database.url is missing in config");
        }
        for (DatabaseType type : values()) {
            if (jdbcUrl.toLowerCase().startsWith(type.urlPrefix)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported database URL: " + jdbcUrl);
    }
}
