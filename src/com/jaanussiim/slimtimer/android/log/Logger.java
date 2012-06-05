package com.jaanussiim.slimtimer.android.log;

import java.io.IOException;

public class Logger {
  private static final String APP = "mTimer";

  public static void d(final String t, final String message) {
    logMessage(APP + ":" + t, message);
  }

  public static void e(final String t, final String message, final IOException e) {
    logMessage(APP + ":" + t, message);
    printStacktrace(e);
  }

  private static void logMessage(final String tag, final String message) {
    System.out.println(tag + " >> " + message);
  }

  public static void printStacktrace(final Throwable e) {
    e.printStackTrace();
  }
}
