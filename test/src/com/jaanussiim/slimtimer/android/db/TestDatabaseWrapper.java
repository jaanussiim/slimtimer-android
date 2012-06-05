package com.jaanussiim.slimtimer.android.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.content.Context;

import com.jaanussiim.slimtimer.android.components.User;

public class TestDatabaseWrapper extends SlimtimerDB {
  private static final Map<String, User> TEST_USERS = new HashMap<String, User>();

  static {
    TEST_USERS.put("user1@test.com", new User(Long.valueOf(1), "user1@test.com", Long.valueOf(100), "unencrypted_password",
        "unencrypted_access_token", false));
  }

  public TestDatabaseWrapper(Context ctx) {
    super(ctx);
  }

  public void createRawLoggedInTestUser() {
    createRawLoggedInTestUser(getRandomUser());
  }

  public void createTestUser() {
    insertRawUser(Long.valueOf(100), "user@test.com", "");
  }

  @Override
  public User getLoggedInUser() {
    User u = super.getLoggedInUser();
    if (u != null) {
      //inject data used for user creation
      User testUser = TEST_USERS.get(u.getEmail());
      u.testSetTestUser(testUser);
    }
    return u;
  }

  private void createRawLoggedInTestUser(User user) {
    insertRawUser(user.getSlimId(), user.getEmail(), user.getPassword());
  }

  private User getRandomUser() {
    Random generator = new Random();
    Object[] values = TEST_USERS.values().toArray();
    return (User) values[generator.nextInt(values.length)];
  }

  public void createLoggedInTestUser() {
    createLoggedInTestUser(getRandomUser());
  }

  private void createLoggedInTestUser(User user) {
    insertUser(user.getEmail(), user.getSlimId(), user.getPassword(), user.getAccessToken());
  }
}
