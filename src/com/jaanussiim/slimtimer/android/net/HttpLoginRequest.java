package com.jaanussiim.slimtimer.android.net;

import java.text.MessageFormat;

import android.content.Context;

import com.google.gson.Gson;
import com.jaanussiim.slimtimer.android.Constants;
import com.jaanussiim.slimtimer.android.components.User;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class HttpLoginRequest extends HttpRequest {
  private SlimtimerDB database;
  private static final String URI = "/users/token";
  private static final String REQUEST = "user:\n  email: {0}\n  password: {1}\napi_key: {2}";
  private final String username;
  private final String password;

  public HttpLoginRequest(final Context context, final String username, final String password) {
    this.username = username;
    this.password = password;
    database = new SlimtimerDB(context);
    setRequestUri(Constants.SERVER_URL + URI);
    setContentType(CONTENT_TYPE_YAML);
    setBody(MessageFormat.format(REQUEST, username, password, Constants.API_KEY));
    setAccept(CONTENT_TYPE_JSON);
    setMethod(METHOD_POST);
  }

  @Override
  protected boolean requestComplete(final int statusCode, final String response) {
    final Gson g = new Gson();
    final LoginResponse fromJson = g.fromJson(response, LoginResponse.class);

    if (statusCode == 500 && getListener() != null) {
      getListener().requestComplete(HttpRequestListener.REQUEST_AUTHENTICATION_ERROR);
    } else if (statusCode == 200) {
      final User u = database.findOrCreateUser(fromJson.getUserId(), username);
      database.setUserLoggedIn(u, password, fromJson.getAccessToken());
      getListener().requestComplete(HttpRequestListener.REQUEST_OK);
    }

    return true;
  }

  public void testSetDatabase(final SlimtimerDB database) {
    this.database = database;
  }
}
