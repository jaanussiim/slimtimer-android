package com.jaanussiim.slimtimer.android.components;

import java.text.MessageFormat;

public class Tag {
  public static final Long ID_UNKNOWN = -1L;
  private final Long id;
  private final String name;

  public Tag(final String name) {
    this(ID_UNKNOWN, name);
  }

  public Tag(final Long id, final String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public String toString() {
    return MessageFormat.format("Tag > {0} - {1}", id, name);
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Tag)) {
      return false;
    }

    final Tag other = (Tag) o;

    return name.equalsIgnoreCase(other.name);
  }

  @Override
  public int hashCode() {
    throw new RuntimeException();
  }

  public Long getDatabaseId() {
    return id;
  }
}
