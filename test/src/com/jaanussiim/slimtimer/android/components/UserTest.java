package com.jaanussiim.slimtimer.android.components;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UserTest {
  @Test
  public void equality() {
    User one = new User(100L, "test@c.com", 1001L, "Testpass", "accessas", false);
    User two = new User(100L, "test@c.com", 1001L, "Testpass", "accessas", false);
    User three = new User(101L, "test@ca.com", 1001L, "Testpaass", "aaccessas", false);
    User four = new User(102L, "teasdst@ca.com", 1002L, "Tesadtpaass", "aaasdccessas", false);

    assertTrue(one.equals(one));
    assertTrue(one.equals(two));
    assertTrue(two.equals(one));
    assertTrue(three.equals(one));
    assertFalse(four.equals(one));
  }
}
