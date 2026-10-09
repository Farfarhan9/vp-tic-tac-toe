package main.exceptions;

/**
 * DatabaseException – typed exception for all database layer failures.
 *
 * Mirrors the style of GameException already in the codebase.
 * Severity levels:
 *   INFO     – informational; the application can continue with defaults.
 *   WARNING  – data may not have been saved; notify the user.
 *   CRITICAL – unrecoverable; the application should abort the operation.
 */
public class DatabaseException extends Exception {

    public enum ErrorSeverity { INFO, WARNING, CRITICAL }

    private final ErrorSeverity severity;

    public DatabaseException(String message, ErrorSeverity severity) {
        super(message);
        this.severity = severity;
    }

    public DatabaseException(String message, Throwable cause, ErrorSeverity severity) {
        super(message, cause);
        this.severity = severity;
    }

    public ErrorSeverity getSeverity() { return severity; }

    @Override
    public String toString() {
        return "[DatabaseException:" + severity + "] " + getMessage()
               + (getCause() != null ? " | Caused by: " + getCause().getMessage() : "");
    }
}