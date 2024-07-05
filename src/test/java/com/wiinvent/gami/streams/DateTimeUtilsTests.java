package com.wiinvent.gami.streams;

import static org.assertj.core.api.Assertions.assertThat;

import com.wiinvent.gami.streams.common.DateTimeUtils;
import com.wiinvent.gami.streams.common.PeriodUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DateTimeUtilsTests {

  @Test
  public void testMapToTimeKey() {

    long day = DateTimeUtils.mapToTimeKey(1647081535000L, PeriodUnit.DAY);
    assertThat(day).isEqualTo(19063);

    // 2022-04-04 00-00-00, Monday
    long week1 = DateTimeUtils.mapToTimeKey(1649005200000L, PeriodUnit.WEEK);
    // 2022-04-11 00-00-00, Monday
    long week2 = DateTimeUtils.mapToTimeKey(1649610000000L, PeriodUnit.WEEK);
    assertThat(week2 - week1).isEqualTo(1);

    // 2022-04-01
    long month1 = DateTimeUtils.mapToTimeKey(1648746000000L, PeriodUnit.MONTH);
    // 2022-07-01
    long month2 = DateTimeUtils.mapToTimeKey(1656608400000L, PeriodUnit.MONTH);
    assertThat(month2 - month1).isEqualTo(3);

    // 2022-04-01
    long quarter1 = DateTimeUtils.mapToTimeKey(1648746000000L, PeriodUnit.QUARTER);
    // 2022-07-01
    long quarter2 = DateTimeUtils.mapToTimeKey(1656608400000L, PeriodUnit.QUARTER);
    assertThat(quarter2 - quarter1).isEqualTo(1);

    long year = DateTimeUtils.mapToTimeKey(1647081535000L, PeriodUnit.YEAR);
    assertThat(year).isEqualTo(2022);
  }
}
