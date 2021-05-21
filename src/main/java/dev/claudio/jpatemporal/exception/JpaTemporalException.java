package dev.claudio.jpatemporal.exception;

@SuppressWarnings("serial")
public class JpaTemporalException extends RuntimeException {

    public JpaTemporalException(final String message) {
        super(message);
    }

    public JpaTemporalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
