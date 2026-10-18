# Personal Expense Tracker (Java + JDBC + MySQL)

A beginner-friendly console project. It lets you record income and expenses, view transactions, see monthly totals, group expenses by category, and delete transactions.

## Requirements
- JDK 17 or newer
- MySQL Server 8 or newer
- Visual Studio Code with the **Extension Pack for Java** and **Maven for Java** extensions
- Internet access for Maven to download Connector/J the first time

## Setup in VS Code (Windows)
1. Extract the ZIP and open the `expense-tracker-vscode` folder in VS Code.
2. Open `src/main/resources/db.properties`.
3. Set `db.user` and `db.password` to the username and password you use to connect to your local MySQL Server. Example: `db.user=root`. Replace the placeholder password; do not put quotes around values.
4. Make sure the MySQL Server service is running.
5. Open Terminal > New Terminal at the project root and run:
   ```powershell
   mvn clean compile exec:java
   ```
Maven automatically downloads the JDBC driver and required build plugins. On its first run this can take a little while.

The application creates the `expense_tracker` database and its `transactions` table automatically. The MySQL account must be allowed to create a database and tables.

## Menu
1. Add income or expense
2. View all transactions
3. Monthly summary (enter month as `YYYY-MM`, or press Enter for current month)
4. Expense breakdown by category
5. Delete a transaction
0. Exit

All SQL values entered by the user are passed through `PreparedStatement` parameters.
