package com.wiinvent.gami.streams.entities;

import com.wiinvent.gami.avro.EventLogParam;
import com.wiinvent.gami.avro.TaskState;
import com.wiinvent.gami.streams.common.DateTimeUtils;
import com.wiinvent.gami.streams.common.PeriodUnit;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Data
@Log4j2
public class Task {

  public static final String DEFAULT_ACTION = "__default_action__";
  public static final String DEFAULT_CONDITION = "__default_condition__";

  private long id;
  private String name;

  private LocalDate startDate;
  private LocalDate endDate;

  @JsonProperty("isRecurring")
  private boolean isRecurring;

  private List<Quest> quests;
  private List<Promotion> promotions;

  private long point;
  private int periodValue;
  private PeriodUnit periodUnit;

  private String action;
  private String condition;

  private long createdAt;
  private long updatedAt;

  private SlidingType slidingType;

  private List<Integer> campaignIds;

  private State state;

  public boolean isEffective() {
    LocalDate now = DateTimeUtils.now();

    return state == State.ACTIVATE
        && (Objects.isNull(startDate) || !now.isBefore(startDate))
        && (Objects.isNull(endDate) || !now.isAfter(endDate));
  }

    public List<Integer> getCampaignIds() {
    return ObjectUtils.defaultIfNull(campaignIds, Collections.emptyList());
  }

  public String getAction() {
    return ObjectUtils.defaultIfNull(action, DEFAULT_ACTION);
  }

  public String getCondition() {
    return ObjectUtils.defaultIfNull(condition, DEFAULT_CONDITION);
  }

  public enum SlidingType {
    NON_OVERLAP,
    OVERLAP,
    TO_NOW,
    TO_EXACTLY_NOW,
    FROM_LAST_COMPLETION,
  }

  public enum State {
    ACTIVATE,
    INACTIVATE,
    COMPLETED,
    DELETED,
  }

    public long totalBonusPoints(TaskState taskState) {
    log.debug("==============>totalBonusPoints start: taskState = {}, promotion = {}, ", taskState, this.promotions);
    List<String> paramValues = taskState.getParams().stream().map(EventLogParam::getValue).collect(toList());
    if (this.promotions.isEmpty()) return 0L;

    long totalBonusPoints = 0L;

    List<Promotion> effectivePromotionsList = this.promotions.stream()
        .filter(Promotion::isEffective)
        .collect(toList());

    log.debug("==============>totalBonusPoints effectivePromotionsList = {} userId = {}, taskId = {}",
        effectivePromotionsList, taskState.getUserId(), taskState.getTaskId());

    LocalDate now = DateTimeUtils.now();

    long dayOfWeekVN = now.getDayOfWeek().getValue();
    long dayOfMonthVN = now.getDayOfMonth();
    long startOfDayVN = DateTimeUtils.getStartOfDay();

    Predicate<Promotion> dayOfWeekPredicate = promotion -> promotion.getPeriodUnit() == Promotion.PeriodUnit.WEEK && promotion.getBonusDay() == dayOfWeekVN;
    Predicate<Promotion> dayOfMonthPredicate = promotion -> promotion.getPeriodUnit() == Promotion.PeriodUnit.MONTH && promotion.getBonusDay() == dayOfMonthVN;
    Predicate<Promotion> specificVNPredicate = promotion -> promotion.getPeriodUnit() == Promotion.PeriodUnit.SPECIFIC && promotion.getBonusDay() == startOfDayVN;

    List<Promotion> allowCurrentPromotions = effectivePromotionsList.stream()
        .filter(dayOfWeekPredicate.or(dayOfMonthPredicate).or(specificVNPredicate))
        .collect(toList());

    log.debug("==============>totalBonusPoints allowCurrentPromotions={}, now = {}, dayOfWeekVN = {}, dayOfMonthVN = {}, startOfDayVN = {}, userId = {}, taskId = {}",
        allowCurrentPromotions, now, dayOfWeekVN, dayOfMonthVN, startOfDayVN, taskState.getUserId(), taskState.getTaskId());

    Map<String, List<Promotion>> paramListPromotionMap = allowCurrentPromotions.stream()
        .flatMap(promotion -> promotion.getType().getParams().stream()
            .flatMap(promotionTypeParam -> Stream.of(promotionTypeParam.getValue().split(","))
                .map(s -> new AbstractMap.SimpleEntry<>(s, promotion))))
        .collect(groupingBy(Map.Entry::getKey,
            mapping(Map.Entry::getValue, Collectors.toList())));

    Map<String, List<Promotion>> promoted = paramListPromotionMap.entrySet().stream()
        .filter(paramListPromotionEntry -> paramValues.contains(paramListPromotionEntry.getKey()) || paramListPromotionEntry.getKey().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<Promotion.BonusType, Long> bonusTypeBonusValueMap = promoted.values().stream()
        .flatMap(Collection::stream).collect(groupingBy(Promotion::getBonusType, summingLong(Promotion::getBonusValue)));

    for (Map.Entry<Promotion.BonusType, Long> entry : bonusTypeBonusValueMap.entrySet()) {
      if (entry.getKey() == Promotion.BonusType.ADD) {
        totalBonusPoints = totalBonusPoints + entry.getValue();
      } else if (entry.getKey() == Promotion.BonusType.MULTI) {
        totalBonusPoints = totalBonusPoints + this.point * (entry.getValue() - 1);
      }
    }
    log.debug("==============>totalBonusPoints end: totalBonusPoints={}, promoted={}, paramValues = {}, userId={}, taskId={}, ",
        totalBonusPoints, promoted, paramValues, taskState.getUserId(), taskState.getTaskId());
    return totalBonusPoints;

  }

}
