package com.jaanussiim.slimtimer.android.utils;

public class ManufacturerExractor {
  public static String getManufacturer() {
    try {
      Class.forName("android.gesture.Gesture");
      ManufacturerExtractorAfter4 ext = new ManufacturerExtractorAfter4();
      return ext.getManufacturer();
    } catch (ClassNotFoundException e) {
      return "Unknown";
    }
  }
}
