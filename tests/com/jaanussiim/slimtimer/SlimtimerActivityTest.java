/*
 * Copyright 2012 JaanusSiim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jaanussiim.slimtimer;

import android.content.Intent;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowIntent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SlimtimerActivityTest {
  private SlimtimerActivity activity;

  @Before
  public void setUp() {
    activity = new SlimtimerActivity();
  }

  @Test
  public void shouldHavePushedLoginActivity() throws Exception {
    activity.onCreate(null);
    ShadowActivity shadowActivity = shadowOf(activity);
    Intent startedIntent = shadowActivity.getNextStartedActivity();
    assertNotNull("Should have started intent", startedIntent);
    ShadowIntent shadowIntent = shadowOf(startedIntent);
    assertThat(shadowIntent.getComponent().getClassName(), equalTo(LoginActivity.class.getName()));
  }
}