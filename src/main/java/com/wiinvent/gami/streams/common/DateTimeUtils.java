package com.wiinvent.gami.streams.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Objects;

public class DateTimeUtils {
  public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
  public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIMEZONE);

  public static void main(String[] args) {
    LocalDateTime now = LocalDateTime.now();

    System.out.println(now.getLong(ChronoField.EPOCH_DAY));
  }

  public static LocalDate toLocalDate(long epochMilli) {
    return toLocalDateTime(epochMilli).toLocalDate();
  }

  public static LocalDateTime toLocalDateTime(long epochMilli) {
    return Instant.ofEpochMilli(epochMilli).atZone(DEFAULT_ZONE_ID).toLocalDateTime();
  }

  public static long toEpochMillis(String date) {
    return toEpochMillis(LocalDate.parse(date));
  }

  public static long toEpochMillis(LocalDate localDate) {
    return localDate.atStartOfDay().atZone(DEFAULT_ZONE_ID).toEpochSecond() * 1000;
  }

  public static long mapToTimeKey(long epochMillis, PeriodUnit periodUnit) {
    LocalDateTime dateTime = DateTimeUtils.toLocalDateTime(epochMillis);

    switch (periodUnit) {
      case HOUR:
        return 0;
    }
  }

  public static long currentTimeMillis() {
    try {
      String now = System.getenv("NOW");
      if (Objects.nonNull(now) && !now.trim().isEmpty()) {
        return toEpochMillis(now);
      }
    } catch (Exception ignored) {

    }
    return System.currentTimeMillis();
  }
}
