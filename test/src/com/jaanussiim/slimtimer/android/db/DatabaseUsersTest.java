package com.jaanussiim.slimtimer.android.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;

import com.jaanussiim.slimtimer.android.components.User;
import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DatabaseUsersTest {
  private static final long TEST_REMOTE_ID = 100L;
  private static final String TEST_USERNAME = "testMeOnce@masta.com";
  private static final String TEST_USERNAME_CHANGED = "testMeTwice@masta.com";
  private SlimtimerDB database;

  @Before
  public void setUp() {
    final Activity context = new Activity();
    database = new TestDatabaseWrapper(context);
    database.open();
  }

  @Test
  public void inEmptyDatabaseNoLoggedInUser() {
    final User u = database.getLoggedInUser();
    assertNull(u);
  }

  @Test
  public void userWithoutPasswordNotLoggedIn() {
    ((TestDatabaseWrapper) database).createTestUser();
    final User u = database.getLoggedInUser();
    assertNull(u);
  }

  @Test
  public void loggedInUserFound() {
    ((TestDatabaseWrapper) database).createLoggedInTestUser();
    final User u = database.getLoggedInUser();
    assertNotNull(u);
    final User testUser = u.testGetTestUser();
    assertEquals(testUser, u);
  }

  @Test
  public void findOorCreateWithNewUser() {
    final User u = database.findOrCreateUser(TEST_REMOTE_ID, TEST_USERNAME);
    assertNotNull(u);
    assertEquals(TEST_REMOTE_ID, u.getSlimId().longValue());
    assertEquals(TEST_USERNAME, u.getEmail());
  }

  @Test
  public void onUsernameChangeUserUpdated() {
    findOorCreateWithNewUser();
    final User u = database.findOrCreateUser(TEST_REMOTE_ID, TEST_USERNAME_CHANGED);
    assertNotNull(u);
    assertEquals(TEST_REMOTE_ID, u.getSlimId().longValue());
    assertEquals(TEST_USERNAME_CHANGED, u.getEmail());
  }

  @After
  public void tearDown() {
    database.close();
  }
}
