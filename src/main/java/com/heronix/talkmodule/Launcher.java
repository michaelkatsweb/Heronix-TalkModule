package com.heronix.talkmodule;

/**
 * Non-JavaFX entry point for running as a fat JAR.
 * JavaFX requires that the main class does NOT extend Application
 * when running from an executable JAR (unnamed module).
 * This class simply delegates to HeronixTalkModuleApplication.main().
 */
public class Launcher {
    public static void main(String[] args) {
        HeronixTalkModuleApplication.main(args);
    }
}
