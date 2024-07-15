package com.wiinvent.gami.streams.entities;

import com.wiinvent.gami.avro.TaskState;
import com.wiinvent.gami.streams.common.PeriodUnit;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

@Data
public class Quest {
  public static int UNBOUND_COUNT = -1;
  public static int DYNAMIC_COUNT = -5;

  private long id;
  private long taskId;
  private QuestType type;
  private int periodValue;
  private PeriodUnit periodUnit;
  private CompletionRule completionRule;
  private AggregateType aggregateType;


  // TODO: I don't know
  public static int callDynamicCount(TaskState taskState, PeriodUnit questPeriodUnit, int questPeriodValue) {
    long startTime = taskState.getWindowStart();
    long endTime = taskState.getWindowEnd();

    return (int) ((endTime - startTime) / (questPeriodValue * questPeriodUnit.toMillis()));

  }

  public AggregateType getAggregateType() {
    return ObjectUtils.defaultIfNull(aggregateType, AggregateType.SUM);
  }

  public enum AggregateType {
    SUM,
    LAST,
  }


  @Data
  public static class CompletionRule {
    private int minCount;
    private int maxCount;
    private boolean isContinuous;

    public int getMinCount(TaskState taskState, PeriodUnit questPeriodUnit, int questPeriodValue) {
      int count = minCount;
      if (count == DYNAMIC_COUNT) {
        count = Quest.callDynamicCount(taskState, questPeriodUnit, questPeriodValue);
      }
      return count;
    }

    public int getMaxCount(TaskState taskState, PeriodUnit questPeriodUnit, int questPeriodValue) {
      int count = maxCount;
      if (count == DYNAMIC_COUNT) {
        count = Quest.callDynamicCount(taskState, questPeriodUnit, questPeriodValue);
      } else if (count < 0) {
        count = Integer.MAX_VALUE;
      }

      return count;
    }

    public int getMaxCount() {
      if (maxCount < 0) {
        return Integer.MAX_VALUE;
      }

      return maxCount;
    }
  }

}
