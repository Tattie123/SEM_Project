package org.example;

import java.sql.SQLException;

// Error handling: call runSafely so reports don't need their own try/catch.
public class ErrorHandler {

    // Something that uses the database and might fail
    public interface DatabaseAction {
        void run() throws SQLException;
    }

    // Runs the task. If it fails, print a message and carry on.
    public static void runSafely(String action, DatabaseAction task) {
        try {
            task.run();
        } catch (SQLException e) {
            handleSQLException(e, action);
        } catch (RuntimeException e) {
            System.out.println("Error while " + action + ": " + e.getMessage());
        }
    }

    // Prints a simple message for a database problem
    public static void handleSQLException(SQLException e, String action) {
        String message;

        if (e.getErrorCode() == 1045) {
            message = "wrong database username or password.";
        } else if (e.getErrorCode() == 1049) {
            message = "database not found.";
        } else if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
            message = "could not connect to the database.";
        } else {
            message = "a database error occurred.";
        }

        System.out.println("Error while " + action + ": " + message);
    }
}
