package com.jaanussiim.slimtimer.android.net_old;

import java.text.MessageFormat;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

import com.jaanussiim.slimtimer.android.Constants;
import com.jaanussiim.slimtimer.android.TimerCore;
import com.jaanussiim.slimtimer.android.components.User;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class LoginRequest extends NetworkRequest {
  private static final String ACCESS_TOKEN_KEY = "access_token";
  private static final String USER_ID_KEY = "user_id";
  private static final String LOG_TAG = "LoginRequest";
  private static final String PATH = "/users/token";
  private final String email;
  private final String password;
  private static final String REQUEST = "user:\n  email: {0}\n  password: {1}\napi_key: {2}";
  private final Context context;
  private final boolean saveCredentials;

  public LoginRequest(final Context activity, final String email, final String password,
      final boolean saveCredentials) {
    this.context = activity;
    this.email = email;
    this.password = password;
    this.saveCredentials = saveCredentials;
  }

  @Override
  public String getRequestYaml() {
    return MessageFormat.format(REQUEST, email, password, APP_KEY);
  }

  @Override
  public String getRequestMethod() {
    //TODO jaanus : check this
    return "POST";
  }

  @Override
  public String getPath() {
    return PATH;
  }

  @Override
  public void handleResponse(final Object responseObject) {
    final JSONObject response = (JSONObject) responseObject;

    String accessToken = null;
    Long userId = null;
    try {
      accessToken = response.getString(ACCESS_TOKEN_KEY);
      userId = Long.parseLong(response.getString(USER_ID_KEY));
    } catch (final JSONException e) {
      //Log.e(LOG_TAG, "error handling response", e);
      //TODO jaanus : handle this
    }

    if (accessToken != null && userId != null) {
      final TimerCore timerCore = TimerCore.getInstance();

      final SlimtimerDB database = new SlimtimerDB(context);
      database.open();

      //TODO jaanus : what to do if null?
      final User user = database.findOrCreateUser(email, userId);
      timerCore.setAccessProps(user, accessToken);
      timerCore.setIsNewUser(user.isNewUser());

      database.close();

      if (saveCredentials) {
        final SharedPreferences preferences = context.getSharedPreferences(
            Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        //TODO jaanus : encrypt these values
        editor.putString(Constants.PREFERENCES_EMAIL_KEY, email);
        editor.putString(Constants.PREFERENCES_PASSWORD_KEY, password);
        editor.commit();
      }

      ((NetworkRequestCaller) context).requestSuccess(NetworkRequestCaller.LOGIN_REQUEST);
    } else {
      ((NetworkRequestCaller) context).requestError(NetworkRequestCaller.LOGIN_ERROR);
      TimerCore.getInstance().setAccessToken("");
    }
  }

  @Override
  public void handleConnectionError() {
    ((NetworkRequestCaller) context).requestError(NetworkRequestCaller.LOGIN_ERROR);
    TimerCore.getInstance().setAccessToken("");
  }
}
