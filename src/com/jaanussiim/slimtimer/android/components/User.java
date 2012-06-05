package com.jaanussiim.slimtimer.android.components;

import java.text.MessageFormat;

public class User {
  public static final long ID_UNKNOWN = -1;
  private final boolean created;
  private final String email;
  private final Long slimId;
  private final Long id;
  private String password;
  private String accesToken;
  private User testUser;

  public User(final long id, final Long slimId, final String email, final boolean created) {
    this.id = id;
    this.slimId = slimId;
    this.email = email;
    this.created = created;
  }

  public User(final Long slimId, final String email) {
    this(ID_UNKNOWN, slimId, email, false);
  }

  public User(Long databaseId, String email, Long remoteId, String password, String accessToken, boolean created) {
    id = databaseId;
    this.created = created;
    this.email = email;
    slimId = remoteId;
    this.password = password;
    this.accesToken = accessToken;
  }

  public Long getSlimId() {
    return slimId;
  }

  public boolean isNewUser() {
    return created;
  }

  public long getDatabaseId() {
    return id;
  }

  @Override
  public String toString() {
    return MessageFormat.format("User: {0} - {1} - {2} - {3}", id, slimId, email, created);
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getAccessToken() {
    return accesToken;
  }

  public void testSetTestUser(User testUser) {
    this.testUser = testUser;
  }

  public User testGetTestUser() {
    return testUser;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof User)) {
      return false;
    }

    if (o == this) {
      return true;
    }

    User other = (User) o;
    return other.slimId.equals(this.slimId);
  }

  @Override
  public int hashCode() {
    throw new RuntimeException("hashCode not implemented");
  }
}
