package com.jaanussiim.slimtimer.android.activities;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import android.app.Activity;
import android.content.Intent;

import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowIntent;

public class ActivityTestUtils {
  public static void namedActivityPushed(final Activity fromActivity, final String nameOfPushedActivity) {
    final ShadowActivity slimtimerShadow = shadowOf(fromActivity);
    final Intent startedIntent = slimtimerShadow.getNextStartedActivity();
    assertNotNull("Intent not started for activity creation?", startedIntent);
    final ShadowIntent shadowIntent = shadowOf(startedIntent);
    assertThat(shadowIntent.getComponent().getClassName(), equalTo(nameOfPushedActivity));
  }
}
