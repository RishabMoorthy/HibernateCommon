package org.common.db;

import org.common.db.config.ConfigLoader;

import java.sql.*;

public class DerbyDb {
    private static final String DB_URL = ConfigLoader.getProperty("derbydb.url");

    public static void configureDb() throws InterruptedException {
        // Use try-with-resources to automatically close connections
        setupDatabase();
        testPersistence();
        shutdownDerby();
        Thread.sleep(5000);
    }

    private static void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (!tableExists(conn, "RECORDS")) {
                System.out.println("First run: Creating table 'RECORDS'...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE RECORDS (id INT PRIMARY KEY, val VARCHAR(50))");
                    // Add some initial data
                    stmt.execute("INSERT INTO RECORDS VALUES (1, 'Initial Data')");
                }
            } else {
                System.out.println("Subsequent run: Table 'RECORDS' already exists. Skipping creation.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void testPersistence() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Add a new record every time we run the app to prove persistence
            long timestamp = System.currentTimeMillis();
            stmt.execute("INSERT INTO RECORDS VALUES (" + (int) (timestamp / 1000) + ", 'Entry at " + timestamp + "')");

            // Read all records
            ResultSet rs = stmt.executeQuery("SELECT * FROM RECORDS");
            System.out.println("--- Current Database Content ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " | Value: " + rs.getString("val"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        // Note: Derby stores table names in UPPERCASE by default
        try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        }
    }

    private static void shutdownDerby() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException se) {
            // XJ015 is the expected "success" code for a full system shutdown
            if (!"XJ015".equals(se.getSQLState())) {
                se.printStackTrace();
            }
        }
    }
}
