package org.common.db;

import org.common.db.entity.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Properties;

/**
 * - Single SessionFactory — dialect and datasource resolved at startup
 * - from whatever DB is in database.url (config_uat / config_local).
 *
 * - JTA transaction config included — toggle OPTION A vs B below.
 */
public class HibernateUtil {

    private static final SessionFactory sessionFactory;
    private static final DatabaseType   databaseType;

    static {
        DataSourceManager dsm = DataSourceManager.getInstance();
        databaseType   = dsm.getDatabaseType();
        sessionFactory = build(dsm);
    }

    private static SessionFactory build(DataSourceManager dsm) {
        try {
            Properties props = new Properties();

            // Dialect auto-set from detected DB type (Oracle / Derby / MySQL / Postgres)
            props.put(Environment.DIALECT,    databaseType.getHibernateDialect());

            // Reuse the HikariCP DataSource — no second pool created
            props.put(Environment.DATASOURCE, dsm.getDataSource());

            // OPTION A: JDBC local transactions (default — works with standalone Jetty)
            props.put(Environment.TRANSACTION_COORDINATOR_STRATEGY,
                    "org.hibernate.resource.transaction.backend.jdbc.internal" +
                    ".JdbcResourceLocalTransactionCoordinatorBuilderImpl");
            props.put("hibernate.physical_naming_strategy", "org.common.db.ConfigDrivenNamingStrategy");
            props.put("hibernate.id.new_generator_mappings", "false");

            // OPTION B: JTA (uncomment if using Atomikos / Bitronix / WildFly)
            // props.put(Environment.TRANSACTION_COORDINATOR_STRATEGY,
            //     "org.hibernate.resource.transaction.backend.jta.internal" +
            //     ".JtaTransactionCoordinatorBuilderImpl");
            // props.put(Environment.JTA_PLATFORM,
            //     "org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform");

            props.put(Environment.SHOW_SQL,    "false");
            props.put(Environment.FORMAT_SQL,  "false");

            String ddlAuto = (databaseType == DatabaseType.DERBY) ? "update" : "none";
            props.put(Environment.HBM2DDL_AUTO, ddlAuto);

            System.out.println("[HibernateUtil] Building SessionFactory -> " + databaseType);

            return new Configuration()
                    .addProperties(props)
                    .addAnnotatedClass(VSDetails.class)
                    .addAnnotatedClass(MasterCatalog.class)
                    .addAnnotatedClass(VsExecutionMode.class)
                    .addAnnotatedClass(VsLiveUrl.class)
                    .addAnnotatedClass(DailyMetrics.class)
                    .addAnnotatedClass(ExecutionDetailsDTO.class)
                    .addAnnotatedClass(VsCatalog.class)
                    .setPhysicalNamingStrategy(new ConfigDrivenNamingStrategy())
                    .buildSessionFactory();

        } catch (Exception e) {
            throw new RuntimeException("Failed to build SessionFactory for " + databaseType, e);
        }
    }

    public static SessionFactory getSessionFactory() { return sessionFactory; }
    public static DatabaseType   getDatabaseType()   { return databaseType;   }

    public static void shutdown() {
        System.out.println("Shutting down hibernate");
        if (sessionFactory != null && !sessionFactory.isClosed()) sessionFactory.close();
        DataSourceManager.getInstance().shutdown();
    }
}
