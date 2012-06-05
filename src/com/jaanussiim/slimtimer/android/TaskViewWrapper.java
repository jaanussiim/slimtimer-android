package com.jaanussiim.slimtimer.android;

import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class TaskViewWrapper {
  private static final String LOG_TAG = "ViewWrapper";
  private final View wrapped;
  private TextView defaultNameView;
  private TextView runningNameView;
  private View defaultView;
  private ViewFlipper viewFlipper;
  private View runningView;
  private static final int DEFAULT_VIEW = 0;
  private static final int RUNNING_VIEW = 1;
  private int position;
  private Chronometer chronometer;

  public TaskViewWrapper(final View wrapped) {
    this.wrapped = wrapped;
  }

  public TextView getDefaultNameView() {
    if (defaultNameView == null) {
      defaultNameView = (TextView) wrapped.findViewById(R.id.task_name_default);
    }

    return defaultNameView;
  }

  public TextView getRunningNameView() {
    if (runningNameView == null) {
      runningNameView = (TextView) wrapped.findViewById(R.id.task_name_running);
    }

    return runningNameView;
  }

  public View getDefaultView() {
    if (defaultView == null) {
      defaultView = wrapped.findViewById(R.id.task_view_default);
    }

    return defaultView;
  }

  public View getRunningView() {
    if (runningView == null) {
      runningView = wrapped.findViewById(R.id.task_view_running);
    }

    return runningView;
  }

  public ViewFlipper getViewFlipper() {
    if (viewFlipper == null) {
      viewFlipper = (ViewFlipper) wrapped.findViewById(R.id.tasks_run_flip);
    }

    return viewFlipper;
  }

  public Chronometer getChronometer() {
    if (chronometer == null) {
      chronometer = (Chronometer) wrapped.findViewById(R.id.task_run_time);
    }

    return chronometer;
  }

  public void makeViewDefault() {
    getViewFlipper().setDisplayedChild(DEFAULT_VIEW);
    wrapped.setBackgroundResource(R.color.default_task_background);
    getDefaultView().setBackgroundResource(R.color.default_task_background);
  }

  public void makeViewRunning(final long runTime) {
    getViewFlipper().setDisplayedChild(RUNNING_VIEW);
    wrapped.setBackgroundResource(R.color.running_task_background);
    getRunningView().setBackgroundResource(R.color.running_task_background);
    final Chronometer c = getChronometer();
    c.setBase(SystemClock.elapsedRealtime() - runTime);
    c.start();
  }

  public void setTaskName(final String name) {
    getDefaultNameView().setText(name);
    getRunningNameView().setText(name);
  }

  public void makeUnselected(final boolean taskIsRunning) {
    if (taskIsRunning) {
      wrapped.setBackgroundResource(R.color.running_task_background);
      getRunningView().setBackgroundResource(R.color.running_task_background);
    } else {
      wrapped.setBackgroundResource(R.color.default_task_background);
      getDefaultView().setBackgroundResource(R.color.default_task_background);
    }
  }

  public void makeSelected(final boolean taskIsRunning) {
    if (taskIsRunning) {
      wrapped.setBackgroundResource(R.color.running_highlight_background);
      getRunningView().setBackgroundResource(R.color.running_highlight_background);
    } else {
      wrapped.setBackgroundResource(R.color.default_highlight_background);
      getDefaultView().setBackgroundResource(R.color.default_highlight_background);
    }
  }

  public void setPosition(final int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }
}
