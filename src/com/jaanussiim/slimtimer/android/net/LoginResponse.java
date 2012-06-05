package com.jaanussiim.slimtimer.android.net;

public class LoginResponse {
  private String error;
  private String access_token;
  private Long user_id;

  public Long getUserId() {
    return user_id;
  }

  public String getError() {
    return error;
  }

  public String getAccessToken() {
    return access_token;
  }

  public void setError(final String error) {
    this.error = error;
  }

  public void setAccess_token(final String accessToken) {
    this.access_token = accessToken;
  }

  public void setUser_id(final Long userId) {
    user_id = userId;
  }
}
