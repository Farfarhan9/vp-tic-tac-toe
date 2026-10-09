package main.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import main.exceptions.GameException;

public class DatabaseConnection {
	//change here for local installation
    private static final String URL = "jdbc:mysql://localhost:3306/tic_tac_toe_db?connectTimeout=3000&socketTimeout=3000";
    private static final String USER = "root";
    private static final String PASSWORD = ""; 

    public static Connection getConnection() throws GameException {
        try {
            // Explicit driver initialization verification block
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new GameException("MySQL JDBC Database Driver missing from application runtime class loader path.", e, GameException.ErrorSeverity.CRITICAL);
        } catch (SQLException e) {
            throw new GameException("Failed to establish link with SQL database. Verify XAMPP port configurations.", e, GameException.ErrorSeverity.WARNING);
        }
    }
    
    public static void testConnection() throws GameException {
        // Safe utilization of standard try-with-resources to enforce immediate stream release
        try (Connection conn = getConnection()) {
            if (conn == null || conn.isClosed()) {
                throw new GameException("Established database channel reports closed or corrupted state.", GameException.ErrorSeverity.WARNING);
            }
            System.out.println("Database layer connectivity verified successfully.");
        } catch (SQLException e) {
            throw new GameException("Resource monitoring failure during channel validation step.", e, GameException.ErrorSeverity.WARNING);
        }
    }
}