package com.jaanussiim.slimtimer.android.components;

import java.text.MessageFormat;

public class Task implements Comparable<Task> {
  public static final Long ID_UNKNOWN = -1L;
  public static final String EDIT_TASK_ID = "edites_task_id";
  private String name;
  private final long id;
  private Long slimId;
  private long startTime;
  private boolean running;
  private boolean completed;
  private boolean inSyncWithServer;
  private boolean deleted;

  public Task(final Long slimId, final String name, final boolean completed) {
    this(ID_UNKNOWN, slimId, name, completed, true, false, false, 0L);
  }

  public Task(final long id, final Long slimId, final String name, final boolean completed,
      final boolean inSyncWithServer, final boolean deleted, final boolean running,
      final long startTime) {
    this.id = id;
    this.slimId = slimId;
    this.name = name;
    this.completed = completed;
    this.inSyncWithServer = inSyncWithServer;
    this.deleted = deleted;
    this.running = running;
    this.startTime = startTime;
  }

  public String getName() {
    return name;
  }

  public Long getSlimId() {
    return slimId;
  }

  @Override
  public String toString() {
    return MessageFormat.format("Task: {0} - {1} - {2} - {3}", id, slimId, name, getRunTime());
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Task)) {
      return false;
    }

    final Task other = (Task) o;

    if (slimId.equals(ID_UNKNOWN) || other.slimId.equals(ID_UNKNOWN)) {
      return id == other.id;
    } else {
      return slimId.equals(other.slimId);
    }
  }

  @Override
  public int hashCode() {
    throw new RuntimeException();
  }

  public boolean isRunning() {
    return running;
  }

  public long getDatabaseId() {
    return id;
  }

  public long getRunTime() {
    if (running) {
      return System.currentTimeMillis() - startTime;
    }

    return 0;
  }

  public long getStartTime() {
    return startTime;
  }

  public void stopRunning() {
    running = false;
  }

  public void startRunning() {
    running = true;
    startTime = System.currentTimeMillis();
  }

  public boolean isCompleted() {
    return completed;
  }

  public int compareTo(final Task another) {
    return name.compareToIgnoreCase(another.name);
  }

  public boolean isInSyncWithServer() {
    return inSyncWithServer;
  }

  public void setName(final String name) {
    inSyncWithServer = false;
    this.name = name;
  }

  public void setCompleted(final boolean completed) {
    inSyncWithServer = false;
    this.completed = completed;
  }

  public void deleteTask(final boolean delete) {
    inSyncWithServer = false;
    deleted = delete;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setSlimId(final long slimId) {
    this.slimId = slimId;
  }

  public void setIsInSync(final boolean inSync) {
    inSyncWithServer = inSync;
  }
}
