package com.stubserver.backend.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DatabaseConfig {

    @Value("${app.db-type:oracle}")
    private String dbType;

    // Oracle
    @Value("${app.datasource.oracle.user:}")
    private String oracleUser;

    @Value("${app.datasource.oracle.password:}")
    private String oraclePassword;

    @Value("${app.datasource.oracle.connect-string:}")
    private String oracleConnectString;

    @Value("${app.datasource.oracle.pool-min:2}")
    private int poolMin;

    @Value("${app.datasource.oracle.pool-max:10}")
    private int poolMax;

    // MySQL
    @Value("${app.datasource.mysql.url:}")
    private String mysqlUrl;

    @Value("${app.datasource.mysql.user:}")
    private String mysqlUser;

    @Value("${app.datasource.mysql.password:}")
    private String mysqlPassword;

    // PostgreSQL
    @Value("${app.datasource.psql.url:}")
    private String psqlUrl;

    @Value("${app.datasource.psql.user:}")
    private String psqlUser;

    @Value("${app.datasource.psql.password:}")
    private String psqlPassword;

    // Apache Derby
    @Value("${app.datasource.derby.url:}")
    private String derbyUrl;

    @Value("${app.datasource.derby.user:}")
    private String derbyUser;

    @Value("${app.datasource.derby.password:}")
    private String derbyPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig cfg = new HikariConfig();
        switch (dbType.toLowerCase()) {
            case "mysql" -> {
                cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
                cfg.setJdbcUrl(mysqlUrl);
                cfg.setUsername(mysqlUser);
                cfg.setPassword(mysqlPassword);
            }
            case "psql" -> {
                cfg.setDriverClassName("org.postgresql.Driver");
                cfg.setJdbcUrl(psqlUrl);
                cfg.setUsername(psqlUser);
                cfg.setPassword(psqlPassword);
            }
            case "derby" -> {
                cfg.setDriverClassName("org.apache.derby.jdbc.ClientDriver");
                cfg.setJdbcUrl(derbyUrl);
                cfg.setUsername(derbyUser);
                cfg.setPassword(derbyPassword);
            }
            default -> {
                cfg.setDriverClassName("oracle.jdbc.OracleDriver");
                cfg.setJdbcUrl("jdbc:oracle:thin:@//" + oracleConnectString);
                cfg.setUsername(oracleUser);
                cfg.setPassword(oraclePassword);
            }
        }
        cfg.setMinimumIdle(poolMin);
        cfg.setMaximumPoolSize(poolMax);
        cfg.setConnectionTimeout(30_000);
        cfg.setIdleTimeout(600_000);
        cfg.setMaxLifetime(1_800_000);
        return new HikariDataSource(cfg);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                        TableNames tableNames) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.common.db.entity");
        HibernateJpaVendorAdapter vendor = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendor);
        Properties jpaProps = new Properties();
        jpaProps.setProperty("hibernate.hbm2ddl.auto", "none");
        jpaProps.setProperty("hibernate.show_sql", "false");
        jpaProps.setProperty("hibernate.physical_naming_strategy",
                "com.stubserver.backend.database.config.DynamicTableNamingStrategy");
        String dialect = switch (dbType.toLowerCase()) {
            case "mysql"  -> "org.hibernate.dialect.MySQLDialect";
            case "psql"   -> "org.hibernate.dialect.PostgreSQLDialect";
            case "derby"  -> "org.hibernate.dialect.DerbyDialect";
            default       -> "org.hibernate.dialect.OracleDialect";
        };
        jpaProps.setProperty("hibernate.dialect", dialect);
        em.setJpaProperties(jpaProps);
        // Configurable table names (deployment-specific, supplied via db-tables.properties)
        DynamicTableNamingStrategy.register("configurable.auditLogs",       tableNames.getAuditLogs());
        DynamicTableNamingStrategy.register("configurable.assignedServices", tableNames.getAssignedServices());
        // Framework shared entities have no @Table annotation — Hibernate uses the class name as logical name
        DynamicTableNamingStrategy.register("VSDetails",       tableNames.getVsDetails());
        DynamicTableNamingStrategy.register("MasterCatalog",   tableNames.getMasterCatalog());
        // Framework shared entities with static table names
        DynamicTableNamingStrategy.register("VsCatalog",       "READYAPI_VS_CATALOG");
        DynamicTableNamingStrategy.register("VsExecutionMode", "VS_EXECUTIONMODE");
        DynamicTableNamingStrategy.register("VsLiveUrl",       "VS_LIVEURLS");
        DynamicTableNamingStrategy.register("DailyMetrics",    "READYAPI_DAILY_METRICS");
        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

}
