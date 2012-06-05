package com.jaanussiim.slimtimer.android.db;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jaanussiim.slimtimer.android.components.User;
import com.jaanussiim.slimtimer.android.utils.Encrypter;
import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DatabaseEncryptionTest {
  private TestDatabaseWrapper w;

  @Before
  public void setUp() {
    w = new TestDatabaseWrapper(new Activity());
    w.open();
  }

  @Test
  public void savedDataEncrypted() {
    w.createLoggedInTestUser();
    SQLiteDatabase database = w.testGetDatabase();
    User u = w.getLoggedInUser();

    final String[] userSelectKeys = new String[] { "email", "password", "access_token" };
    final Cursor c = database.query("users", userSelectKeys, "password != ''", null, null, null, null);
    c.moveToFirst();

    String email = c.getString(0);
    String password = c.getString(1);
    String accessToken = c.getString(2);
    c.close();

    assertEquals(u.getEmail(), Encrypter.decrypt(email));
    assertEquals(u.getPassword(), Encrypter.decrypt(password));
    assertEquals(u.getAccessToken(), Encrypter.decrypt(accessToken));
  }
}
