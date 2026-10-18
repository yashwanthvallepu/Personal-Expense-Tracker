package com.expensetracker;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/** Reads database settings and creates the database/table on first run. */
public final class Database {
    private static final Properties PROPERTIES = loadProperties();
    private static final String SERVER_URL = PROPERTIES.getProperty("db.serverUrl", "jdbc:mysql://localhost:3306/").trim();
    private static final String DATABASE_NAME = PROPERTIES.getProperty("db.name", "expense_tracker").trim();
    private static final String USER = PROPERTIES.getProperty("db.user", "root").trim();
    private static final String PASSWORD = PROPERTIES.getProperty("db.password", "");

    private Database() { }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = Database.class.getResourceAsStream("/db.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing src/main/resources/db.properties file.");
            }
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(SERVER_URL + DATABASE_NAME, USER, PASSWORD);
    }

    public static void initialize() throws SQLException {
        if (PASSWORD.contains("CHANGE_THIS_TO_YOUR_MYSQL_PASSWORD")) {
            throw new IllegalStateException("Open src/main/resources/db.properties and replace db.password with your MySQL password.");
        }

        // Create the database using a server-level connection first.
        try (Connection connection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            // DATABASE_NAME is a project constant read from a local properties file.
            if (!DATABASE_NAME.matches("[A-Za-z0-9_]+")) {
                throw new IllegalStateException("Database name may contain only letters, digits, and underscores.");
            }
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME);
        }

        // Create the table and seed demo categories are not needed because categories are free text.
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    type ENUM('INCOME','EXPENSE') NOT NULL,
                    category VARCHAR(80) NOT NULL,
                    amount DECIMAL(12,2) NOT NULL,
                    description VARCHAR(255) NOT NULL DEFAULT '',
                    txn_date DATE NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT chk_transaction_amount CHECK (amount > 0),
                    INDEX idx_transaction_date (txn_date)
                )
                """);
        }
    }
}
