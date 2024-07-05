package com.wiinvent.gami.streams.entities;

import com.wiinvent.gami.streams.common.DateTimeUtils;
import lombok.Data;

import java.time.LocalDate;
import java.util.Objects;


@Data
public class Promotion {
  private long id;
  private long taskId;
  private PromotionType type;
  private PeriodUnit periodUnit;
  private long bonusDay;
  private BonusType bonusType;
  private int bonusValue;
  private LocalDate startDate;
  private LocalDate endDate;
  private TaskPromotionState state;
  private Boolean isNoEndDate;


  public enum BonusType {
    ADD,
    MULTI
  }

  public enum PeriodUnit {
    WEEK,
    MONTH,
    SPECIFIC
  }

  public enum TaskPromotionState {
    ACTIVE,
    INACTIVE,
    DELETED
  }

  public boolean isEffective() {
    LocalDate now = DateTimeUtils.now();
    if (isNoEndDate) {
      return state == TaskPromotionState.ACTIVE && (Objects.isNull(startDate) || !now.isBefore(startDate));
    } else
      return state == TaskPromotionState.ACTIVE
          && (Objects.isNull(startDate) || !now.isBefore(startDate))
          && (Objects.isNull(endDate) || !now.isAfter(endDate));
  }
}
