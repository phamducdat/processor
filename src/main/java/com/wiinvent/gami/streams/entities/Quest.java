package com.wiinvent.gami.streams.entities;

import lombok.Data;

@Data
public class Quest {
  public static int UNBOUND_COUNT = -1;
  public static int DYNAMIC_COUNT = -5;

  private long id;
  private long taskId;
  private QuestType type;
  private int periodValue;
  private P

}
