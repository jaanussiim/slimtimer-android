package com.jaanussiim.slimtimer.android.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;

import com.jaanussiim.slimtimer.android.Constants;
import com.jaanussiim.slimtimer.android.components.User;
import com.jaanussiim.slimtimer.android.db.TestDatabaseWrapper;
import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HttpLoginRequestTest implements HttpRequestListener {
  private static final String SUCCESS_RESPONSE = "{user_id: 56782, access_token: \"9309d0f92d4db5e4bbd5f32a83afba35d446b6b4\"}";
  private static final String TEST_USERNAME = "username@test.com";
  private static final String TEST_PASSWORD = "fookakaPassas";
  private HttpRequest request;
  private int status;
  private TestDatabaseWrapper databaseWrapper;

  @Before
  public void setUp() {
    request = new HttpLoginRequest(new Activity(), TEST_USERNAME, TEST_PASSWORD);
    request.setListener(this);

    databaseWrapper = new TestDatabaseWrapper(new Activity());
    databaseWrapper.open();
    ((HttpLoginRequest) request).testSetDatabase(databaseWrapper);

    status = 0;
  }

  @Test
  public void correctUrl() {
    final String expectedUrl = Constants.SERVER_URL + "/users/token";
    final String generated = request.testGetUri();
    assertNotNull(generated);
    assertEquals(expectedUrl, generated);
  }

  @Test
  public void correctContentType() {
    assertEquals(HttpRequest.CONTENT_TYPE_YAML, request.testGetContentType());
  }

  @Test
  public void correctBodyGenerated() {
    final String expectedBody = "user:\n  email: " + TEST_USERNAME + "\n  password: " + TEST_PASSWORD + "\napi_key: " + Constants.API_KEY;
    final String body = request.testGetBody();
    assertEquals(expectedBody, body);
  }

  @Test
  public void correctAccepts() {
    assertEquals(HttpRequest.CONTENT_TYPE_JSON, request.testGetAccept());
  }

  @Test
  public void correctMethod() {
    assertEquals(HttpRequest.METHOD_POST, request.testGetMethod());
  }

  @Test
  public void authenticationErrorHandling() {
    request.requestComplete(500, "{error: \"Authentication failed\"}");
    assertEquals(HttpRequestListener.REQUEST_AUTHENTICATION_ERROR, status);
  }

  @Test
  public void successHandling() {
    request.requestComplete(200, SUCCESS_RESPONSE);
    assertEquals(HttpRequestListener.REQUEST_OK, status);
  }

  @Test
  public void networkError() {
    request.networkError();
    assertEquals(HttpRequestListener.REQUEST_NETWORK_ERROR, status);
  }

  @Test
  public void onSuccessUserAddedToDatabaseAsLoggedIn() {
    User loggedInUser = databaseWrapper.getLoggedInUser();
    assertNull(loggedInUser);

    request.requestComplete(200, SUCCESS_RESPONSE);
    loggedInUser = databaseWrapper.getLoggedInUser();
    assertNotNull("Login request should have created logged in user", loggedInUser);

    assertEquals(new Long(56782), loggedInUser.getSlimId());
    assertEquals("9309d0f92d4db5e4bbd5f32a83afba35d446b6b4", loggedInUser.getAccessToken());
    assertEquals(TEST_USERNAME, loggedInUser.getEmail());
    assertEquals(TEST_PASSWORD, loggedInUser.getPassword());
  }

  @Override
  public void requestComplete(final int status) {
    this.status = status;
  }
}
