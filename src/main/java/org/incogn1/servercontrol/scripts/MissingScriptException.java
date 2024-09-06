package org.incogn1.servercontrol.scripts;

public class MissingScriptException extends Exception {

    public MissingScriptException() {}

    public MissingScriptException(String message) {
        super(message);
    }

    public MissingScriptException(Throwable cause) {
        super(cause);
    }

    public MissingScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
