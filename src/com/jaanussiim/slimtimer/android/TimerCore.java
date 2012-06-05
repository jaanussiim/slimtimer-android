package com.jaanussiim.slimtimer.android;

import com.jaanussiim.slimtimer.android.components.User;

//TODO jaanus : loose this
public class TimerCore {
  private static TimerCore instance;
  private String accessToken;
  private User user;
  private boolean newUser;

  public static TimerCore getInstance() {
    if (instance == null) {
      instance = new TimerCore();
    }

    return instance;
  }

  public void setAccessProps(final User user, final String accessToken) {
    this.user = user;
    this.accessToken = accessToken;
  }

  public Long getUserid() {
    return user.getSlimId();
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setIsNewUser(final boolean newUser) {
    this.newUser = newUser;
  }

  public boolean isNewUser() {
    return newUser;
  }

  public User getUser() {
    return user;
  }

  public void setUser(final User user) {
    this.user = user;
  }

  public void clear() {
    instance = null;
  }

  public void setAccessToken(final String token) {
    accessToken = token;
  }
}
