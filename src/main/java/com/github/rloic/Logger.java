package com.github.rloic;

public abstract class Logger {

    private static Logger logger = new Logger() {
    };

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_ERROR = "\u001b[38;5;1m";
    private static final String ANSI_WARN = "\u001b[38;5;172m";
    private static final String ANSI_INFO = "";
    private static final String ANSI_DEBUG = "\u001b[38;5;4m";
    private static final String ANSI_TRACE = "\u001b[38;5;30m";

    private Logger() {
    }

    public static void level(Logger level) {
        logger = level;
    }

    public static void err(Object o) {
        logger._err(o);
    }

    public static void warn(Object o) {
        logger._warn(o);
    }

    public static void info(Object o) {
        logger._info(o);
    }

    public static void debug(Object o) {
        logger._debug(o);
    }

    public static void trace(Object o) {
        logger._trace(o);
    }

    public static boolean isError() {
        return logger._isError();
    }

    public static boolean isWarn() {
        return logger._isWarn();
    }

    public static boolean isInfo() {
        return logger._isInfo();
    }

    public static boolean isDebug() {
        return logger._isDebug();
    }

    public static boolean isTrace() {
        return logger._isTrace();
    }

    protected void _trace(Object o) {
    }

    protected void _debug(Object o) {
    }

    protected void _info(Object o) {
    }

    protected void _warn(Object o) {
    }

    protected void _err(Object o) {
    }

    protected boolean _isError() {
        return false;
    }

    protected boolean _isWarn() {
        return false;
    }

    protected boolean _isInfo() {
        return false;
    }

    protected boolean _isTrace() {
        return false;
    }

    protected boolean _isDebug() {
        return false;
    }

    public static class ErrorLogger extends Logger {

        public static Logger ERROR = new ErrorLogger();

        private ErrorLogger() {
        }

        @Override
        final protected void _err(Object o) {
            System.out.println("[" + ANSI_ERROR + "err" + ANSI_RESET + "]\t" + o);
        }

        @Override
        final protected boolean _isError() {
            return true;
        }
    }

    public static class WarnLogger extends ErrorLogger {

        public static Logger WARN = new WarnLogger();

        private WarnLogger() {
        }

        @Override
        final protected void _warn(Object o) {
            System.out.println("[" + ANSI_WARN + "warn" + ANSI_RESET + "]\t" + o);
        }

        @Override
        final protected boolean _isWarn() {
            return true;
        }
    }

    public static class InfoLogger extends WarnLogger {
        public static Logger INFO = new InfoLogger();

        private InfoLogger() {
        }

        @Override
        final protected void _info(Object o) {
            System.out.println("[" + ANSI_INFO + "info" + ANSI_RESET + "]\t" + o);
        }

        @Override
        final protected boolean _isInfo() {
            return super._isInfo();
        }
    }

    public static class DebugLogger extends InfoLogger {
        public static Logger DEBUG = new DebugLogger();

        private DebugLogger() {
        }

        @Override
        final protected void _debug(Object o) {
            System.out.println("[" + ANSI_DEBUG + "debug" + ANSI_RESET + "]\t" + o);
        }

        @Override
        final protected boolean _isDebug() {
            return true;
        }
    }

    public static class TraceLogger extends DebugLogger {

        public static Logger TRACE = new TraceLogger();

        private TraceLogger() {
        }

        @Override
        final protected void _trace(Object o) {
            System.out.println("[" + ANSI_TRACE + "trace" + ANSI_RESET + "]\t" + o);
        }

        @Override
        final protected boolean _isTrace() {
            return true;
        }
    }


}
