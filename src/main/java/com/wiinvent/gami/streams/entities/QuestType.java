package com.wiinvent.gami.streams.entities;

import com.wiinvent.gami.avro.EventLog;
import com.wiinvent.gami.avro.EventLogParam;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class QuestType {

  private int id;
  private String name;
  private String code;
  private String externalId;
  private boolean parameterized;

  private List<QuestTypeParam> params;

  public boolean isEventMatched(EventLog eventLog) {
    if (!Objects.equals(externalId, eventLog.getType())) {
      return false;
    }

    return !parameterized || isParamsMatched(eventLog);
  }

  private boolean isParamsMatched(EventLog eventLog) {
    List<EventLogParam> eventLogParams =
        ObjectUtils.defaultIfNull(eventLog.getParams(), Collections.emptyList());

    Map<String, String> valueByName =
        eventLogParams.stream()
            .collect(Collectors.toMap(EventLogParam::getName, EventLogParam::getValue));

    for (QuestTypeParam param : params) {
      String actualValue = valueByName.get(param.getExternalId());
      if (Objects.isNull(actualValue) || !param.isMatched(actualValue)) {
        return false;
      }
    }

    return true;
  }

}
