package de.craftsblock.craftsnet.utils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private boolean debug;

    public Logger(boolean debug) {
        this.debug = debug;
    }

    public void info(String text) {
        log("\u001b[34;1mINFO ", text);
    }

    public void warning(String text) {
        log("\u001b[33mWARN ", text);
    }

    public void error(String text) {
        log("\u001b[31;1mERROR", text);
    }

    public void error(Exception exception) {
        log("\u001b[31;1mERROR", exception.getMessage());
        exception.printStackTrace();
    }

    public void error(Exception exception, String comment) {
        log("\u001b[31;1mERROR", comment + " > " + exception.getMessage());
        exception.printStackTrace();
    }

    public void debug(String text) {
        if (debug)
            log("\u001b[38;5;147mDEBUG", text);
    }

    private void log(String prefix, String text) {
        System.out.println("\u001b[38;5;228m" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + prefix + " \u001b[38;5;219m| \u001b[36m" + Thread.currentThread().getName() + "\u001b[38;5;252m: " + text + "\u001b[0m");
    }

}
