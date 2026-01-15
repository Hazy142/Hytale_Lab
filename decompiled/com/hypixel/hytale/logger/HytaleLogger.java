/*
 * Decompiled with CFR 0.152.
 */
package com.hypixel.hytale.logger;

import com.google.common.flogger.AbstractLogger;
import com.google.common.flogger.LogContext;
import com.google.common.flogger.LoggingApi;
import com.google.common.flogger.backend.Platform;
import com.google.common.flogger.parser.DefaultPrintfMessageParser;
import com.google.common.flogger.parser.MessageParser;
import com.hypixel.hytale.logger.backend.HytaleConsole;
import com.hypixel.hytale.logger.backend.HytaleFileHandler;
import com.hypixel.hytale.logger.backend.HytaleLogManager;
import com.hypixel.hytale.logger.backend.HytaleLoggerBackend;
import com.hypixel.hytale.logger.backend.HytaleUncaughtExceptionHandler;
import com.hypixel.hytale.logger.util.LoggerPrintStream;
import io.sentry.IScopes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import javax.annotation.Nonnull;

public class HytaleLogger
extends AbstractLogger<Api> {
    private static final Map<String, HytaleLogger> CACHE;
    private static final HytaleLogger LOGGER;
    static final NoOp NO_OP;
    @Nonnull
    private final HytaleLoggerBackend backend;

    public static void init() {
        HytaleFileHandler fileHandler = HytaleFileHandler.INSTANCE;
        HytaleConsole console = HytaleConsole.INSTANCE;
        LOGGER.at(Level.INFO).log("Logger Initialized");
    }

    public static void replaceStd() {
        if (!HytaleLoggerBackend.isJunitTest()) {
            System.setOut(new LoggerPrintStream(HytaleLogger.get("SOUT"), Level.INFO));
            System.setErr(new LoggerPrintStream(HytaleLogger.get("SERR"), Level.SEVERE));
        }
    }

    public static HytaleLogger getLogger() {
        return LOGGER;
    }

    @Nonnull
    public static HytaleLogger forEnclosingClass() {
        String className = Platform.getCallerFinder().findLoggingClass(HytaleLogger.class);
        String loggerName = HytaleLogger.classToLoggerName(className);
        return HytaleLogger.get(loggerName);
    }

    @Nonnull
    public static HytaleLogger forEnclosingClassFull() {
        String loggingClass = Platform.getCallerFinder().findLoggingClass(HytaleLogger.class);
        return HytaleLogger.get(loggingClass);
    }

    @Nonnull
    public static HytaleLogger get(String loggerName) {
        return CACHE.computeIfAbsent(loggerName, key -> new HytaleLogger(HytaleLoggerBackend.getLogger(key)));
    }

    private HytaleLogger(@Nonnull HytaleLoggerBackend backend) {
        super(backend);
        this.backend = backend;
    }

    @Override
    public Api at(@Nonnull Level level) {
        return this.isLoggable(level) ? new Context(level) : NO_OP;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Nonnull
    public Level getLevel() {
        return this.backend.getLevel();
    }

    public void setLevel(@Nonnull Level level) {
        this.backend.setLevel(level);
    }

    @Nonnull
    public HytaleLogger getSubLogger(String name) {
        return new HytaleLogger(this.backend.getSubLogger(name));
    }

    public void setSentryClient(@Nonnull IScopes scope) {
        this.backend.setSentryClient(scope);
    }

    public void setPropagatesSentryToParent(boolean propagate) {
        this.backend.setPropagatesSentryToParent(propagate);
    }

    @Nonnull
    private static String classToLoggerName(@Nonnull String className) {
        int lastIndexOf = className.lastIndexOf(46);
        String loggerName = lastIndexOf < 0 || className.length() <= lastIndexOf + 1 ? className : className.substring(lastIndexOf + 1);
        return loggerName;
    }

    static {
        System.setProperty("java.util.logging.manager", HytaleLogManager.class.getName());
        HytaleUncaughtExceptionHandler.setup();
        LogManager logManager = LogManager.getLogManager();
        if (!logManager.getClass().getName().equals(HytaleLogManager.class.getName())) {
            throw new IllegalStateException("Log manager wasn't set! Please ensure HytaleLogger is the first logger to be initialized or\nuse `System.setProperty(\"java.util.logging.manager\", HytaleLogManager.class.getName());` at the start of your application.\nLog manager is: " + String.valueOf(logManager));
        }
        CACHE = new ConcurrentHashMap<String, HytaleLogger>();
        LOGGER = new HytaleLogger(HytaleLoggerBackend.getLogger());
        NO_OP = new NoOp();
    }

    public static interface Api
    extends LoggingApi<Api> {
    }

    final class Context
    extends LogContext<HytaleLogger, Api>
    implements Api {
        private Context(Level level) {
            super(level, false);
        }

        @Override
        @Nonnull
        protected HytaleLogger getLogger() {
            return HytaleLogger.this;
        }

        @Override
        @Nonnull
        protected Api api() {
            return this;
        }

        @Override
        @Nonnull
        protected Api noOp() {
            return NO_OP;
        }

        @Override
        protected MessageParser getMessageParser() {
            return DefaultPrintfMessageParser.getInstance();
        }
    }

    private static final class NoOp
    extends LoggingApi.NoOp<Api>
    implements Api {
        private NoOp() {
        }
    }
}

