package com.wiinvent.gami.streams.common;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

@UtilityClass
@Slf4j
public class StringUtils {

  public static final String SET_DELIMITER = ",";

  public static StringSet parseStringSet(String value) {
    String[] pieces = value.split(SET_DELIMITER);
    StringSet stringSet = new StringSet();
    try {
      for (String piece : pieces) {
        stringSet.add(piece.trim());
      }

      return stringSet;
    } catch (Exception e) {
      log.warn(e.getMessage());
    }
    return stringSet;
  }


  public static class StringSet extends HashSet<String> {}
}
