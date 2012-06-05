package com.jaanussiim.slimtimer.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

import com.jaanussiim.slimtimer.android.Constants;
import com.jaanussiim.slimtimer.android.SlimtimerTasksList;
import com.jaanussiim.slimtimer.android.components.User;
import com.jaanussiim.slimtimer.android.db.SlimtimerDB;

public class SlimtimerActivity extends Activity {
  private static final String LOG_TAG = "Slimtimer";
  private SlimtimerDB db;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (db == null) {
      db = new SlimtimerDB(this);
      db.open();
    }

    //do reporting code move to database
    final SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
    final String repCode = sharedPreferences.getString(Constants.PREFERENCES_REPORTING_CODE, "");
    if (!"".equals(repCode)) {
      db.saveReportingCode(repCode);
      final Editor edit = sharedPreferences.edit();
      edit.putString(Constants.PREFERENCES_REPORTING_CODE, "");
      edit.commit();
    }

    final User loggedInUser = db.getLoggedInUser();

    Intent nextActivityStart;
    //email and password can be empty, when database contains unencrypted data
    if (loggedInUser == null || "".equals(loggedInUser.getEmail()) || "".equals(loggedInUser.getPassword())) {
      nextActivityStart = new Intent(SlimtimerActivity.this, LoginActivity.class);
    } else {
      nextActivityStart = new Intent(SlimtimerActivity.this, SlimtimerTasksList.class);
    }

    startActivity(nextActivityStart);
    finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    db.close();
  }

  protected void testSetDatabase(final SlimtimerDB db) {
    this.db = db;
  }
}
