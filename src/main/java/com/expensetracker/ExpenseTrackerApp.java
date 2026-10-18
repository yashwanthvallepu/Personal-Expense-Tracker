package com.expensetracker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Console-based Personal Expense Tracker using Java, JDBC and MySQL. */
public class ExpenseTrackerApp {
    private static final Scanner INPUT = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Database.initialize();
            System.out.println("Connected to MySQL. Expense Tracker is ready!");
            runMenu();
        } catch (IllegalStateException e) {
            System.out.println("SETUP PROBLEM: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: " + e.getMessage());
            System.out.println("Check that MySQL Server is running and db.properties has the correct username/password.");
        }
    }

    private static void runMenu() {
        while (true) {
            System.out.println("\n========== PERSONAL EXPENSE TRACKER ==========");
            System.out.println("1. Add income or expense");
            System.out.println("2. View all transactions");
            System.out.println("3. Monthly summary");
            System.out.println("4. Expense breakdown by category");
            System.out.println("5. Delete a transaction");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            String choice = INPUT.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addTransaction();
                    case "2" -> viewTransactions();
                    case "3" -> showMonthlySummary();
                    case "4" -> showExpenseBreakdown();
                    case "5" -> deleteTransaction();
                    case "0" -> {
                        System.out.println("Thank you for using Expense Tracker!");
                        return;
                    }
                    default -> System.out.println("Please enter one of the listed options.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static void addTransaction() throws SQLException {
        System.out.print("Type (I = Income, E = Expense): ");
        String typeInput = INPUT.nextLine().trim().toUpperCase();
        String type;
        if (typeInput.equals("I")) {
            type = "INCOME";
        } else if (typeInput.equals("E")) {
            type = "EXPENSE";
        } else {
            System.out.println("Invalid type. Enter I or E.");
            return;
        }

        System.out.print("Category (Food, Travel, Salary, etc.): ");
        String category = INPUT.nextLine().trim();
        if (category.isBlank()) {
            System.out.println("Category cannot be empty.");
            return;
        }

        BigDecimal amount;
        try {
            System.out.print("Amount in rupees: ");
            amount = new BigDecimal(INPUT.nextLine().trim()).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Amount must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid amount, such as 250 or 250.50.");
            return;
        }

        System.out.print("Description (optional): ");
        String description = INPUT.nextLine().trim();
        System.out.print("Date (YYYY-MM-DD, Enter for today): ");
        String dateInput = INPUT.nextLine().trim();
        LocalDate date;
        try {
            date = dateInput.isEmpty() ? LocalDate.now() : LocalDate.parse(dateInput);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date. Use YYYY-MM-DD, for example 2026-10-12.");
            return;
        }

        String sql = "INSERT INTO transactions (type, category, amount, description, txn_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, type);
            statement.setString(2, category);
            statement.setBigDecimal(3, amount);
            statement.setString(4, description);
            statement.setDate(5, Date.valueOf(date));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    System.out.println("Transaction saved successfully. ID: " + keys.getLong(1));
                } else {
                    System.out.println("Transaction saved successfully.");
                }
            }
        }
    }

    private static void viewTransactions() throws SQLException {
        String sql = "SELECT id, type, category, amount, description, txn_date FROM transactions ORDER BY txn_date DESC, id DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            System.out.println("\nID | TYPE | CATEGORY | AMOUNT | DATE | DESCRIPTION");
            System.out.println("---------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d | %s | %s | Rs.%s | %s | %s%n",
                    rs.getLong("id"), rs.getString("type"), rs.getString("category"),
                    rs.getBigDecimal("amount"), rs.getDate("txn_date"), rs.getString("description"));
            }
            if (!found) System.out.println("No transactions yet. Choose option 1 to add one.");
        }
    }

    private static YearMonth readMonth() {
        System.out.print("Month (YYYY-MM, Enter for current month): ");
        String value = INPUT.nextLine().trim();
        if (value.isEmpty()) return YearMonth.now();
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid month. Current month will be used.");
            return YearMonth.now();
        }
    }

    private static void showMonthlySummary() throws SQLException {
        YearMonth month = readMonth();
        String sql = """
            SELECT
                COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
                COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expenses
            FROM transactions WHERE txn_date BETWEEN ? AND ?
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(month.atDay(1)));
            statement.setDate(2, Date.valueOf(month.atEndOfMonth()));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                BigDecimal income = rs.getBigDecimal("income");
                BigDecimal expenses = rs.getBigDecimal("expenses");
                BigDecimal balance = income.subtract(expenses);
                BigDecimal savingsRate = income.signum() > 0
                    ? balance.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                System.out.println("\nMONTHLY SUMMARY: " + month);
                System.out.println("Total income   : Rs." + income);
                System.out.println("Total expenses : Rs." + expenses);
                System.out.println("Net balance    : Rs." + balance);
                System.out.println("Net / income % : " + savingsRate + "%");
            }
        }
    }

    private static void showExpenseBreakdown() throws SQLException {
        YearMonth month = readMonth();
        String sql = """
            SELECT category, SUM(amount) AS total
            FROM transactions
            WHERE type = 'EXPENSE' AND txn_date BETWEEN ? AND ?
            GROUP BY category
            ORDER BY total DESC
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(month.atDay(1)));
            statement.setDate(2, Date.valueOf(month.atEndOfMonth()));
            try (ResultSet rs = statement.executeQuery()) {
                System.out.println("\nEXPENSE BREAKDOWN: " + month);
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-20s Rs.%s%n", rs.getString("category"), rs.getBigDecimal("total"));
                }
                if (!found) System.out.println("No expenses recorded for this month.");
            }
        }
    }

    private static void deleteTransaction() throws SQLException {
        System.out.print("Enter transaction ID to delete: ");
        long id;
        try {
            id = Long.parseLong(INPUT.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("ID must be a number.");
            return;
        }
        System.out.print("Delete transaction " + id + "? (y/N): ");
        if (!INPUT.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Delete cancelled.");
            return;
        }
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            statement.setLong(1, id);
            int rows = statement.executeUpdate();
            System.out.println(rows == 1 ? "Transaction deleted." : "No transaction found with that ID.");
        }
    }
}
