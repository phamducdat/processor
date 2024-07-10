package com.wiinvent.gami.streams.common;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

@Slf4j
@UtilityClass
public class NumberUtils {

  public static final String RANGE_DELIMITER = "-";
  public static final String SET_DELIMITER = ",";

  public static IntRange parseIntRange(String value) {
    String[] pieces = value.split(RANGE_DELIMITER);
    try {
      if (pieces.length == 2) {
        int min = Integer.parseInt(pieces[0]);
        int max = Integer.parseInt(pieces[1]);
        if (min < max) {
          return new IntRange(min, max);
        }
      }
    } catch (Exception e) {
      log.debug(e.getMessage());
    }
    return null;
  }

  public static IntSet parseIntSet(String value) {
    String[] pieces = value.split(SET_DELIMITER);
    IntSet intSet = new IntSet();

    try {
      for (String piece : pieces) {
        intSet.add(Integer.parseInt(piece.trim()));
      }

      return intSet;
    } catch (Exception e) {
      log.warn(e.getMessage());
    }
    return intSet;
  }

  public static class IntRange {
    private int min;
    private int max;

    public IntRange(int min, int max) {
      this.min = min;
      this.max = max;
    }

    public boolean contains(int value) {
      return min <= value && value <= max;
    }
  }

  public static class IntSet extends HashSet<Integer> {
  }

}
