package main.exceptions;

import java.time.LocalDateTime;

public class GameException extends Exception {
    private final LocalDateTime errorTimestamp;
    private final ErrorSeverity severity;

    public enum ErrorSeverity {
        INFO, WARNING, CRITICAL
    }

    public GameException(String message, ErrorSeverity severity) {
        super(message);
        this.severity = severity;
        this.errorTimestamp = LocalDateTime.now();
    }

    public GameException(String message, Throwable cause, ErrorSeverity severity) {
        super(message, cause);
        this.severity = severity;
        this.errorTimestamp = LocalDateTime.now();
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] Failure: %s", errorTimestamp, severity, getMessage());
    }
}