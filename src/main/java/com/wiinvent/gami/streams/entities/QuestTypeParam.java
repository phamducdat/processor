package com.wiinvent.gami.streams.entities;

import com.wiinvent.gami.streams.common.NumberUtils;
import com.wiinvent.gami.streams.common.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.wiinvent.gami.streams.common.NumberUtils.IntRange;
import com.wiinvent.gami.streams.common.NumberUtils.IntSet;
import com.wiinvent.gami.streams.common.StringUtils.StringSet;


import java.util.Objects;

@Data
@Slf4j
public class QuestTypeParam {
  private String name;
  private String externalId;
  private DataType dataType;
  private Operator operator;

  private String value;

  public boolean isMatched(Object actualValue) {
    try {
      switch (dataType) {
        case STRING:
          return isStringMatched(actualValue.toString());
        case INTEGER:
          return isIntegerMatched(Integer.parseInt(actualValue.toString()));
        default:
          return false;
      }
    } catch (Exception e) {
      log.warn(e.getMessage());
      return false;
    }
  }

  private boolean isStringMatched(String actualValue) {
    switch (operator) {
      case EQUAL:
        return Objects.equals(value, actualValue);
      case NOT_EQUAL:
        return !Objects.equals(value, actualValue);
      case IN_SET:
        StringSet stringSet = StringUtils.parseStringSet(value);
        return stringSet.contains(actualValue);
      default:
        return false;
    }
  }

  // TODO: separate into another class ???
  private boolean isIntegerMatched(int actualValue) {
    switch (operator) {
      case EQUAL:
        return Integer.parseInt(value) == actualValue;
      case NOT_EQUAL:
        return Integer.parseInt(value) != actualValue;
      case IN_SET:
        IntSet intSet = NumberUtils.parseIntSet(value);
        return intSet.contains(actualValue);
      case IN_RANGE:
        IntRange intRange = NumberUtils.parseIntRange(value);
        return Objects.nonNull(intRange) && intRange.contains(actualValue);
      default:
        return false;
    }
  }


  public enum DataType {
    STRING,
    NUMERIC,
    INTEGER,
    REAL
  }

  public enum Operator {
    EQUAL,
    NOT_EQUAL,
    IN_SET,
    IN_RANGE
  }
}
