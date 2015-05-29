package net.gini.android.visiontest;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;


public class LogbackConfigurator {

    private FileAppender<ILoggingEvent> fileAppender;

    public void configureBasicLogging() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();

        // setup LogcatAppender
        final PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
        layoutEncoder.setContext(lc);
        layoutEncoder.setPattern("[%thread] %msg%n");
        layoutEncoder.start();

        final LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(layoutEncoder);
        logcatAppender.start();

        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(logcatAppender);
    }

    public void enableFileLogging(Context context){
        if (fileAppender == null) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            // setup FileAppender
            final PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
            layoutEncoder.setContext(lc);
            layoutEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            layoutEncoder.start();

            fileAppender = new FileAppender<>();
            fileAppender.setContext(lc);
            final File storageDirectory = context.getExternalFilesDir(null);
            final File logFile = new File(storageDirectory, "visiontest.log");
            fileAppender.setFile(logFile.getAbsolutePath());
            fileAppender.setEncoder(layoutEncoder);
        }

        fileAppender.start();
        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);
    }

    public void disableFileLogging(){
        if (fileAppender != null) {
            fileAppender.stop();
            final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.detachAppender(fileAppender);
        }
    }
}
