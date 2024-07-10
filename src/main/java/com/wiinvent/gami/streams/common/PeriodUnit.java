package com.wiinvent.gami.streams.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.*;

public enum PeriodUnit {
  INSTANT(ChronoUnit.MILLIS, t -> t),
  HOUR(ChronoUnit.HOURS, t -> t),
  DAY(ChronoUnit.DAYS, t -> t),
  WEEK(ChronoUnit.WEEKS, TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
  MONTH(ChronoUnit.MONTHS, TemporalAdjusters.firstDayOfMonth()) {
    @Override
    public long toAdaptiveMillis(long milestone) {
      LocalDate localDate = DateTimeUtils.toLocalDate(milestone);
      if (localDate.getDayOfMonth() == 1) {
        return 31 * 86400L * 1000; // max days of a month
      } else if (localDate.getDayOfMonth() == 31) {
        return 28 * 86400L * 1000; // min days of a month
      }
      return super.toAdaptiveMillis(milestone);
    }
  },
  QUARTER(IsoFields.QUARTER_YEARS, t -> t.with(IsoFields.DAY_OF_QUARTER, 1L)),
  YEAR(ChronoUnit.YEARS, TemporalAdjusters.firstDayOfYear()),
  ALL_THE_TIME(ChronoUnit.MILLENNIA, t -> t),
  AGAIN(null, null), // Initialize appropriately or handle in a specific way
  UNSET(ChronoUnit.MILLENNIA, t -> t);

  PeriodUnit(TemporalUnit unit, TemporalAdjuster adjuster) {
    this.unit = unit;
    this.adjuster = adjuster;
  }

  protected TemporalUnit unit;
  protected TemporalAdjuster adjuster;

  public TemporalUnit getUnit() {
    return unit;
  }

  public long toMillis() {
    return unit.getDuration().toMillis();
  }

  public long toAdaptiveMillis() {
    return toAdaptiveMillis(DateTimeUtils.currentTimeMillis());
  }

  public long toAdaptiveMillis(long milestone) {
    return toMillis();
  }

  public long adjust(long epochMillis) {
    LocalDate date = DateTimeUtils.toLocalDate(epochMillis).with(adjuster);
    return DateTimeUtils.toEpochMillis(date);
  }

}
