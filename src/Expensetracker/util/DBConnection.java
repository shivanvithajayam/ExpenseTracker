package Expensetracker.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBConnection {

    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection == null) {

                File dbFile = new File(
                        "C:\\Users\\shiva\\OneDrive\\Documents\\Desktop\\Expense_tracker_app\\expenses.db");

                System.out.println("DATABASE LOCATION: " + dbFile.getAbsolutePath());

                connection = DriverManager.getConnection(
                        "jdbc:sqlite:" + dbFile.getAbsolutePath());

                createTables();
                System.out.println("Database connected successfully");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    private static void createTables() {
        try (Statement st = connection.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "date TEXT," +
                    "category TEXT," +
                    "type TEXT," +
                    "method TEXT," +
                    "amount REAL," +
                    "note TEXT" +
                    ")";

            st.execute(sql);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
