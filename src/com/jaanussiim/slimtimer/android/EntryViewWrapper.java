package com.jaanussiim.slimtimer.android;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class EntryViewWrapper {
  private final View wrapped;
  private TextView taskNameView;
  private TextView entryTimeView;
  private TextView commentView;
  private final Context ctx;
  private TextView tagsView;
  private int position;

  public EntryViewWrapper(final View row, final Context cxt) {
    wrapped = row;
    this.ctx = cxt;
  }

  public void setTaskName(final String taskName) {
    getTaskNameView().setText(taskName);
  }

  public void setBackgroundResource(final int colorId) {
    wrapped.setBackgroundResource(colorId);
    getTaskNameView().setBackgroundResource(colorId);
  }

  private TextView getTaskNameView() {
    if (taskNameView == null) {
      taskNameView = (TextView) wrapped.findViewById(R.id.entry_task_name);
    }
    return taskNameView;
  }

  public void setEntryTimeText(final String runTimeDisplayString) {
    getEntriTimeView().setText(runTimeDisplayString);
  }

  private TextView getEntriTimeView() {
    if (entryTimeView == null) {
      entryTimeView = (TextView) wrapped.findViewById(R.id.entry_time_view);
    }

    return entryTimeView;
  }

  public void setComment(final String comment) {
    final TextView commentV = getCommentView();
    if ("".equals(comment.trim())) {
      commentV.setTextAppearance(ctx, R.style.entry_list_no_comment);
      commentV.setText("(no comment added)");
    } else {
      commentV.setTextAppearance(ctx, R.style.entry_list_comment);
      commentV.setText(comment);
    }
  }

  public TextView getCommentView() {
    if (commentView == null) {
      commentView = (TextView) wrapped.findViewById(R.id.entry_comment);
    }
    return commentView;
  }

  public void setTagsString(final String tagsString) {
    final TextView tagsV = getTagsView();
    if ("".equals(tagsString.trim())) {
      tagsV.setTextAppearance(ctx, R.style.entry_list_no_tags);
      tagsV.setText("(no tags added)");
    } else {
      tagsV.setTextAppearance(ctx, R.style.entry_list_tags);
      tagsV.setText(tagsString);
    }
  }

  private TextView getTagsView() {
    if (tagsView == null) {
      tagsView = (TextView) wrapped.findViewById(R.id.entry_tags);
    }

    return tagsView;
  }

  public void makeSelected() {
    setBackgroundResource(R.color.entry_list_active_background);
  }

  public void makeUnselected(final boolean running) {
    if (running) {
      setBackgroundResource(R.color.entry_list_unfinished_background);
    } else {
      setBackgroundResource(R.color.entry_list_default_background);
    }
  }

  public void setPosition(final int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }
}
