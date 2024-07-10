package com.wiinvent.gami.streams.common;

import java.util.*;

public class SequenceUtils {

  private static List<List<Long>> findContinuousElements(Collection<Long> elements) {
    List<Long> elementList = new ArrayList<>(elements);
    Collections.sort(elementList);

    List<List<Long>> result = new ArrayList<>();
    result.add(new ArrayList<>());

    for (int i = 0; i < elementList.size(); i++) {
      long currElement = elementList.get(i);

      // Add element to last sublist
      List<Long> lastSublist = result.get(result.size() - 1);
      lastSublist.add(currElement);

      if (i < elementList.size() - 1) {
        long nextElement = elementList.get(i + 1);
        if (currElement + 1 != nextElement) {
          result.add(new ArrayList<>());
        }
      }
    }
    return result;
  }

  public static List<Long> findLongestContinuousChain(Collection<Long> elements) {
    List<List<Long>> result = findContinuousElements(elements);

    result.sort(Comparator.comparingInt(List::size));
    return result.get(result.size() - 1);
  }


}
