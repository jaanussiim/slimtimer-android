package com.jaanussiim.slimtimer.android.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Entry {
  public static final String EDIT_INTENT_ID = "edit_entry_id";
  private final Long id;
  private final User user;
  private final Task task;
  private String comment;
  private Tag[] tags;
  private Date startTime;
  private Date endTime;
  private boolean running;

  public Entry(final Long id, final User user, final Task task, final String comment,
      final Tag[] tags) {
    this(id, user, task, comment, tags, null, null);
  }

  public Entry(final Long id, final User user, final Task task, final String comment,
      final Tag[] tags, final Date startTime, final Date endTime) {
    this.id = id;
    this.user = user;
    this.task = task;
    this.comment = comment;
    this.tags = tags;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public String getTaskName() {
    return task.getName();
  }

  public boolean isRunning() {
    //TODO jaanus : check this 'running'
    return endTime == null || running;
  }

  public Date getStartTime() {
    return startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public long getRunTime() {
    if (endTime == null) {
      return System.currentTimeMillis() - startTime.getTime();
    }

    return endTime.getTime() - startTime.getTime();
  }

  public String getComment() {
    return comment;
  }

  public Tag[] getTags() {
    return tags;
  }

  public Task getTask() {
    return task;
  }

  public void setStartTime(final Date time) {
    startTime = time;
  }

  public String getTagsAsJoinedStrings() {
    final StringBuffer buff = new StringBuffer();
    for (int i = 0; i < tags.length; i++) {
      buff.append(tags[i].getName());
      if (i != tags.length - 1) {
        buff.append(", ");
      }
    }
    return buff.toString();
  }

  public Long getDatabaseId() {
    return id;
  }

  public void setEndTime(final Date time) {
    endTime = time;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  public void parseTagsFromString(final String string) {
    final String tagsString = string.trim();
    if ("".equals(tagsString)) {
      return;
    }

    final String[] split = tagsString.split(",");
    final List<Tag> parsed = new ArrayList<Tag>();
    for (String s : split) {
      s = s.trim();
      if ("".equals(s)) {
        continue;
      }

      final Tag added = new Tag(s);
      if (!parsed.contains(added)) {
        parsed.add(added);
      }
    }

    tags = parsed.toArray(new Tag[parsed.size()]);
  }

  public void setIsRunning(final boolean running) {
    this.running = running;
  }
}
