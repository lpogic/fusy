package fusy.compile;

public class DebuggerException extends RuntimeException {

    public DebuggerException() {
    }

    public DebuggerException(String message) {
        super(message);
    }

    public DebuggerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DebuggerException(Throwable cause) {
        super(cause);
    }

    public DebuggerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
