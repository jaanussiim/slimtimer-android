package com.jaanussiim.slimtimer.android.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jaanussiim.slimtimer.android.R;
import com.jaanussiim.slimtimer.android.SlimtimerTasksList;
import com.jaanussiim.slimtimer.android.net.HttpLoginRequest;
import com.jaanussiim.slimtimer.android.net.HttpRequest;
import com.jaanussiim.slimtimer.android.net.HttpRequestListener;

public class LoginActivity extends Activity implements OnCancelListener, HttpRequestListener {
  private static final String T = "SlimtimerActivity";

  private static final int DIALOG_LOGGING_IN = 0;
  private static final int DIALOG_LOGIN_ERROR = 1;

  private EditText email;
  private EditText password;
  private CheckBox rememberMe;

  private HttpRequest loginRequest;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.login);

    email = (EditText) findViewById(R.id.email_edit);
    password = (EditText) findViewById(R.id.password_edit);
    rememberMe = (CheckBox) findViewById(R.id.remember_me);
    final TextView createAccountText = (TextView) findViewById(R.id.login_create_account_text);
    createAccountText.setText(Html.fromHtml(getString(R.string.create_account_link)));
    createAccountText.setMovementMethod(LinkMovementMethod.getInstance());

    final Button login = (Button) findViewById(R.id.login_button);
    login.setOnClickListener(new View.OnClickListener() {

      public void onClick(final View v) {
        final String emailString = email.getText().toString().trim();
        final String passwordString = password.getText().toString().trim();

        if (!"".equals(emailString) && !"".equals(passwordString)) {
          showDialog(0);
          loginRequest = new HttpLoginRequest(LoginActivity.this, emailString, passwordString);
          loginRequest.setListener(LoginActivity.this);
          loginRequest.execute();
        }
      }
    });
  }

  @Override
  protected Dialog onCreateDialog(final int id) {
    switch (id) {
    case DIALOG_LOGGING_IN:
      final ProgressDialog dialog = new ProgressDialog(this);
      dialog.setMessage(getText(R.string.logging_in_message));
      dialog.setIndeterminate(true);
      dialog.setCancelable(true);
      dialog.setOnCancelListener(this);
      return dialog;
    case DIALOG_LOGIN_ERROR:
    }

    return null;
  }

  public void onCancel(final DialogInterface dialog) {
    if (loginRequest != null) {
      loginRequest.cancel();
    }
  }

  public void requestError(final int errorCode) {
    removeDialog(DIALOG_LOGGING_IN);
  }

  public void requestComplete(final int status) {
    removeDialog(DIALOG_LOGGING_IN);
    if (status == REQUEST_OK) {
      final Intent intent = new Intent(this, SlimtimerTasksList.class);
      startActivity(intent);
    } else if (status == REQUEST_AUTHENTICATION_ERROR) {
      showErrorToastWithMessage(R.string.login_error_authentication);
    } else if (status == REQUEST_NETWORK_ERROR) {
      showErrorToastWithMessage(R.string.login_error_network);
    }
  }

  private void showErrorToastWithMessage(final int messageId) {
    runOnUiThread(new Runnable() {
      public void run() {
        final Animation shake = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.shake);
        findViewById(R.id.email_edit).startAnimation(shake);
        findViewById(R.id.password_edit).startAnimation(shake);
        Toast.makeText(LoginActivity.this, messageId, Toast.LENGTH_SHORT).show();
      }
    });
  }
}
