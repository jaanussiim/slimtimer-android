package com.jaanussiim.slimtimer.android.activities;

import static com.jaanussiim.slimtimer.android.activities.ActivityTestUtils.namedActivityPushed;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.jaanussiim.slimtimer.android.Constants;
import com.jaanussiim.slimtimer.android.SlimtimerTasksList;
import com.jaanussiim.slimtimer.android.db.TestDatabaseWrapper;
import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SlimtimerActivityTest {
  private SlimtimerActivity activity;
  private TestDatabaseWrapper databaseWrapper;

  @Before
  public void setUp() {
    activity = new SlimtimerActivity();
    databaseWrapper = new TestDatabaseWrapper(new Activity());
    databaseWrapper.open();
    activity.testSetDatabase(databaseWrapper);
  }

  @Test
  public void withoutLoggedInUserLoginActivityPushed() {
    activity.onCreate(null);
    namedActivityPushed(activity, LoginActivity.class.getName());
  }

  @Test
  public void withoutEncryptedUsernameLoginPushed() {
    databaseWrapper.createRawLoggedInTestUser();
    activity.testSetDatabase(databaseWrapper);

    activity.onCreate(null);
    namedActivityPushed(activity, LoginActivity.class.getName());
  }

  @Test
  public void withUsernameTasksListPushed() {
    databaseWrapper.createLoggedInTestUser();
    activity.onCreate(null);
    namedActivityPushed(activity, SlimtimerTasksList.class.getName());
  }

  @Test
  public void withReportingCodeInSharedPrefsMoveItToDatabase() {
    final String testCode = "asdkjkasjdadasdasd";
    final SharedPreferences sharedPreferences = activity.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(Constants.PREFERENCES_REPORTING_CODE, testCode);
    editor.commit();

    activity.onCreate(null);

    final String codeInPreferences = sharedPreferences.getString(Constants.PREFERENCES_REPORTING_CODE, "");
    assertEquals("", codeInPreferences);

    final String codeInDatabase = databaseWrapper.getReportingCode();
    assertEquals(testCode, codeInDatabase);
  }

  @Test
  public void withNoReportingCodeInPrefsDatabaseValueNotOverwritten() {
    final String testCode = "kjfjklsdjfklsjlfkjsdkfjksd";
    databaseWrapper.saveReportingCode(testCode);

    activity.onCreate(null);

    final String savedValue = databaseWrapper.getReportingCode();
    assertEquals(testCode, savedValue);
  }
}
