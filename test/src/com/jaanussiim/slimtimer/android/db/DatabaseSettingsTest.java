package com.jaanussiim.slimtimer.android.db;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DatabaseSettingsTest {
  private SlimtimerDB database;

  @Before
  public void setUp() {
    database = new SlimtimerDB(new Activity());
    database.open();
  }

  @Test
  public void reportingCodePersisted() {
    String testCode = "testasCodas";
    database.saveReportingCode(testCode);
    String saved = database.getReportingCode();
    assertEquals(testCode, saved);
  }

  @Test
  public void ifNoReportingCodeReturnEmptyString() {
    String saved = database.getReportingCode();
    assertEquals("", saved);
  }

  @After
  public void tearDown() {
    database.close();
  }
}
