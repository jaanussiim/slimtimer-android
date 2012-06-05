package com.jaanussiim.slimtimer.android.activities;

import static com.jaanussiim.slimtimer.android.activities.ActivityTestUtils.namedActivityPushed;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jaanussiim.slimtimer.android.SlimtimerTasksList;
import com.jaanussiim.slimtimer.android.net.HttpRequestListener;
import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LoginActivityTest {
  private LoginActivity activity;

  @Before
  public void setUp() {
    activity = new LoginActivity();
  }

  @Test
  public void onSuccesTasksActivityPushed() {
    activity.onCreate(null);
    activity.requestComplete(HttpRequestListener.REQUEST_OK);
    namedActivityPushed(activity, SlimtimerTasksList.class.getName());
  }
}
