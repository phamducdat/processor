package com.wiinvent.gami.streams.processors;

import com.wiinvent.gami.avro.*;
import com.wiinvent.gami.streams.common.DateTimeUtils;
import com.wiinvent.gami.streams.common.PeriodUnit;
import com.wiinvent.gami.streams.common.SequenceUtils;
import com.wiinvent.gami.streams.entities.Quest;
import com.wiinvent.gami.streams.entities.Task;
import com.wiinvent.gami.streams.processors.exeptions.NoQuestCompletionFoundException;
import com.wiinvent.gami.streams.repositories.ITaskRepository;
import com.wiinvent.gami.streams.repositories.TaskNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Configuration
@Slf4j
public class TaskCompletionStreamProcessor {
    private final ThreadLocal<String> traceId = new ThreadLocal<>();

    @Autowired
    private ITaskRepository taskRepository;

    @Autowired
    private Topics topics;
    @Value("${stream.group-key}")
    private String groupByKey;

    @Value("${stream.store-name}")
    private String storeName;

    private void addTraceIdToLog() {
        try {
            traceId.set(UUID.randomUUID().toString());
            MDC.put("traceId", traceId.get());
            MDC.put("spanId", traceId.get());
        } finally {
            traceId.remove();
        }
    }

    @Bean
    public KStream<String, EventLog> eventLogStream(StreamsBuilder streamsBuilder) {
        addTraceIdToLog();

        return streamsBuilder.stream(topics.getEventLogTopic(), Consumed.with(Serdes.String(), null))
            .filter((key, value) -> value instanceof EventLog)
            .mapValues(
                (key, value) -> {
                    EventLog eventLog = (EventLog) value;
                    eventLog.setProcessedAt(DateTimeUtils.currentTimeMillis());
                    return eventLog;
                })
            .peek((key, value) -> log.debug(
                "{}: {} --> {}",
                DateTimeUtils.toLocalDateTime(value.getOccurredAt()),
                key,
                value));
    }

    @Bean
    public KStream<String, TaskEvent> taskEventStream(KStream<String, EventLog> eventLogStream) {
        return eventLogStream
            .filter((key, value) -> (value.getUserId() != null && !value.getUserId().trim().isEmpty()))
            .flatMap(
                (key, eventLog) -> {
                    List<Task> tasks = taskRepository.findByEvent(eventLog);
                    log.debug("==============>taskEventStream tasks = {}", tasks);
                    return tasks.stream()
                        .filter(
                            task -> {
                                if (!task.isEffective()) {
                                    // TODO: log
                                }
                                return task.isEffective();
                            })
                        .map(
                            task -> {
                                String newKey = String.format("%s|%s", eventLog.getUserId(), task.getId());
                                TaskEvent taskEvent =
                                    TaskEvent.newBuilder().setTaskId(task.getId()).setEvent(eventLog).build();
                                return KeyValue.pair(newKey, taskEvent);
                            })
                        .collect(Collectors.toSet());
                });
    }

    @Bean
    public KStream<String, TaskState> taskStateStream(KStream<String, TaskEvent> taskEventStream) {
        KTable<String, TaskState> taskStateTable =
            taskEventStream
                .groupByKey(Grouped.with(groupByKey, Serdes.String(), null))
                .aggregate(
                    TaskState.newBuilder()::build,
                    (key, taskEvent, taskState) -> {
                        log.debug("******** TaskState Aggregate ********");
                        taskState.setCompleted(false);

                        Task task;

                        try {
                            task = taskRepository.findById(taskEvent.getTaskId());
                        } catch (TaskNotFoundException e) {
                            log.warn("Task {} not found: {}", taskEvent.getTaskId(), e.toString());
                            return taskState;
                        }

                        if (!task.isRecurring() && taskState.getNumCompletions() >= 1) {
                            log.info(
                                "Task {} is not recurring. User {} already done. Stop here",
                                taskState.getTaskId(),
                                taskState.getUserId());
                            return taskState;
                        }

                        EventLog event = taskEvent.getEvent();

                        taskState.setUserId(event.getUserId());
                        taskState.setTaskId(task.getId());
                        taskState.setParams(event.getParams());

                        List<Quest> quests = task.getQuests();

                        adjustWindowTime(task, taskState);

                        quests.forEach(
                            quest -> {
                                String questKey = String.valueOf(quest.getId());

                                QuestState questState =
                                    taskState
                                        .getQuestStates()
                                        .getOrDefault(
                                            questKey,
                                            QuestState.newBuilder()
                                                .setTaskId(task.getId())
                                                .setUserId(event.getUserId())
                                                .build());

                                taskState.getQuestStates().put(questKey, questState);

                                if (!quest.getType().isEventMatched(event)) {
                                    log.debug(
                                        "Event type {} not matched with quest {}. Ignored.",
                                        event.getType(),
                                        quest.getId());
                                    return;
                                }

                                /**
                                 * check quest period unit = DAY && task period unit = MONTH
                                 * ===> mỗi ngày add 1 lần vào current record
                                 */
                                List<QuestRecord> currentRecords = questState.getCurrentRecords();
                                if (quest.getPeriodUnit() == PeriodUnit.DAY && task.getPeriodUnit() == PeriodUnit.MONTH) {
                                    Map<Long, List<QuestRecord>> listMap = groupByTimeKeyAddCurrentRecord(currentRecords, quest.getPeriodUnit());
                                    long timeKey = DateTimeUtils.mapToTimeKey(event.getOccurredAt(), quest.getPeriodUnit());
                                    if (!listMap.containsKey(timeKey)) {
                                        currentRecords.add(
                                            QuestRecord.newBuilder()
                                                .setOccurredAt(event.getOccurredAt())
                                                .setValue(event.getValue())
                                                .build());
                                    }
                                } else if (quest.getPeriodUnit() == PeriodUnit.INSTANT && task.getPeriodUnit() == PeriodUnit.UNSET) {
                                    if (quest.getCompletionRule().getMinCount() <= event.getValue() && event.getValue() <= quest.getCompletionRule().getMaxCount()) {
                                        currentRecords.add(
                                            QuestRecord.newBuilder()
                                                .setOccurredAt(event.getOccurredAt())
                                                .setValue(event.getValue())
                                                .build());
                                    }
                                } else if (quest.getPeriodUnit() == PeriodUnit.AGAIN) {
                                    Map<Long, List<QuestRecord>> listMap = groupByTimeKeyAddCurrentRecord(currentRecords, PeriodUnit.DAY);
                                    long timeKey = DateTimeUtils.mapToTimeKey(event.getOccurredAt(), PeriodUnit.DAY);
                                    if (!listMap.containsKey(timeKey)) {
                                        currentRecords.add(
                                            QuestRecord.newBuilder()
                                                .setOccurredAt(event.getOccurredAt())
                                                .setValue(event.getValue())
                                                .build());
                                    }
                                } else {
                                    currentRecords.add(
                                        QuestRecord.newBuilder()
                                            .setOccurredAt(event.getOccurredAt())
                                            .setValue(event.getValue())
                                            .build());
                                }

                                if (quest.getPeriodUnit() == PeriodUnit.AGAIN) {
                                    checkQuestCompletedAgain(quest, taskState, questState);
                                } else {
                                    checkQuestCompleted(quest, taskState, questState);
                                }
                            });

                        boolean taskCompleted = checkTaskCompleted(taskState, event);

                        if (taskCompleted) {
                            slideWindowTime(task, taskState);
                        }

                        return taskState;
                    },
                    Materialized.<String, TaskState, KeyValueStore<Bytes, byte[]>>as(storeName)
                        .withCachingDisabled() //  .withKeySerde(Serdes.String()) .withLoggingEnabled()
                );

        return taskStateTable.toStream();
    }


    private void slideWindowTime(Task task, TaskState taskState) {
        // TODO
    }

    private void adjustWindowTime(Task task, TaskState taskState) {
        // TODO
    }

    private boolean checkTaskCompleted(TaskState taskState, EventLog eventLog) {
        Collection<QuestState> questStates = taskState.getQuestStates().values();
        try {
            questStates.stream()
                .map(
                    questState ->
                        questState.getCompletions().stream()
                            .filter(questCompletion ->
                                questCompletion.getStatus() == QuestCompletionStatus.COMPLETED)
                            .collect(
                                Collectors.collectingAndThen(
                                    toList(),
                                    list -> {
                                        if (!list.isEmpty())
                                            return list.get(0);
                                        else
                                            throw new NoQuestCompletionFoundException();
                                    })))
                .collect(toList())
                .forEach(questCompletion -> questCompletion.setStatus(QuestCompletionStatus.DONE));

            taskState.setCompleted(true);
            taskState.setLastCompletionTime(eventLog.getOccurredAt() + 1);
            taskState.setNumCompletions(taskState.getNumCompletions() + 1);
        } catch (NoQuestCompletionFoundException ignored) {
            taskState.setCompleted(false);
        }

        return taskState.getCompleted();
    }

    private Map<Long, List<QuestRecord>> groupByTimeKeyAddCurrentRecord(List<QuestRecord> records, PeriodUnit periodUnit) {
        return records.stream()
            .collect(
                groupingBy(
                    record -> {
                        long timeKey = DateTimeUtils.mapToTimeKey(record.getOccurredAt(), periodUnit);
                        log.debug("==============>groupByTimeKeyAddCurrentRecord timeKey = {}", timeKey);
                        return timeKey;
                    }
                )
            );
    }

    private void checkQuestCompletedAgain(Quest quest, TaskState taskState, QuestState questState) {
        log.debug("==============>checkQuestCompleteAgain quest = {}", quest);

        // Check completion rule
        Quest.CompletionRule rule = quest.getCompletionRule();
        List<QuestRecord> selectedRecords = questState.getCurrentRecords();

        long totalValue = getTotalValueQuestAgain(quest.getAggregateType(), selectedRecords);

        int minCount = rule.getMinCount(taskState, quest.getPeriodUnit(), quest.getPeriodValue());
        int maxCount = rule.getMaxCount(taskState, quest.getPeriodUnit(), quest.getPeriodValue());

        boolean completed = totalValue >= minCount && totalValue <= maxCount;

        log.debug("==============>checkQuestCompleteAgain completed = {}", completed);

        if (completed) {
            QuestCompletion questCompletion = QuestCompletion.newBuilder()
                .setStatus(QuestCompletionStatus.COMPLETED)
                .setRecords(selectedRecords)
                .build();

            questState.setCompletions(Collections.singletonList(questCompletion));
            questState.getCurrentRecords().remove(0);
        }
    }

    private boolean checkQuestCompleted(Quest quest, TaskState taskState, QuestState questState) {
        log.debug("==============>checkQuestComplete quest = {}", quest);

        LocalDate windowStart = DateTimeUtils.toLocalDate(taskState.getWindowStart());
        LocalDate windowEnd = DateTimeUtils.toLocalDate(taskState.getWindowEnd());
        List<QuestRecord> records = questState.getCurrentRecords();

        // Group by time key, each group has at least 1 record ? For sure??
        Map<Long, List<QuestRecord>> recordGroups = groupByTimeKey(windowStart, windowEnd, records, quest.getPeriodUnit());

        // Check completion rule
        Quest.CompletionRule rule = quest.getCompletionRule();
        List<QuestRecord> selectedRecords = getSelectedRecords(rule.isContinuous(), recordGroups);

        long totalValue = getTotalValue(quest.getAggregateType(), selectedRecords);

        int minCount = rule.getMinCount(taskState, quest.getPeriodUnit(), quest.getPeriodValue());
        int maxCount = rule.getMaxCount(taskState, quest.getPeriodUnit(), quest.getPeriodValue());

        boolean completed = totalValue >= minCount && totalValue <= maxCount;

        log.debug(
            "\n====== checkquestcompleted_{} *** Quest: {} " + "\n\n----> [Result]: {} <= {} <= {}: {}",
            questState.getUserId(),
            quest,
            minCount,
            totalValue,
            maxCount,
            completed);
        if (completed) {
            QuestCompletion questCompletion = QuestCompletion.newBuilder()
                .setStatus(QuestCompletionStatus.COMPLETED)
                .setRecords(selectedRecords)
                .build();

            questState.setCompletions(Collections.singletonList(questCompletion));
            questState.getCurrentRecords().clear();
        }

        return completed;

    }

    private long getTotalValue(Quest.AggregateType aggregateType, List<QuestRecord> selectedRecords) {
        switch (aggregateType) {
            case LAST:
                return selectedRecords.isEmpty()
                    ? 0
                    : selectedRecords.get(selectedRecords.size() - 1).getValue();
            case SUM:
                return selectedRecords.stream().mapToLong(QuestRecord::getValue).sum();
            default:
                return 0;
        }
    }

    private long getTotalValueQuestAgain(Quest.AggregateType aggregateType, List<QuestRecord> selectedRecords) {
        switch (aggregateType) {
            case LAST:
                return 0;
            case SUM:
                log.debug("=========== selectedRecords.size()" + selectedRecords.size());
                if (selectedRecords.size() < 2) {
                    return 1;
                }
                QuestRecord questRecordOld = selectedRecords.get(selectedRecords.size() - 2);
                QuestRecord questRecordNew = selectedRecords.get(selectedRecords.size() - 1);

                LocalDate dateOld = DateTimeUtils.toLocalDateTime(questRecordOld.getOccurredAt()).toLocalDate();
                LocalDate dateNew = DateTimeUtils.toLocalDateTime(questRecordNew.getOccurredAt()).toLocalDate();
                return Period.between(dateOld, dateNew).getDays() - 1;
            default:
                return 0;
        }
    }

    private List<QuestRecord> getSelectedRecords(
        boolean isKeysContinuous, Map<Long, List<QuestRecord>> recordGroups) {
        // Get selected keys
        Collection<Long> selectedKeys = isKeysContinuous ? SequenceUtils.findLongestContinuousChain(recordGroups.keySet())
            : recordGroups.keySet();

        // Get selected records respectively
        return selectedKeys.stream()
            .map(
                key -> {
                    List<QuestRecord> groupRecords = recordGroups.get(key);
                    return groupRecords.get(recordGroups.size() - 1);
                }
            )
            .sorted(Comparator.comparingLong(QuestRecord::getOccurredAt))
            .collect(toList());
    }

    private Map<Long, List<QuestRecord>> groupByTimeKey(
        LocalDate windowStart,
        LocalDate windowEnd,
        List<QuestRecord> records,
        PeriodUnit periodUnit
    ) {
        return records.stream()
            .filter(
                record -> {
                    LocalDate occurredDate = DateTimeUtils.toLocalDate(record.getOccurredAt());
                    return occurredDate.toEpochDay() >= windowStart.toEpochDay()
                        && occurredDate.toEpochDay() <= windowEnd.toEpochDay();
                })
            .collect(
                groupingBy(
                    record -> {
                        long timeKey = DateTimeUtils.mapToTimeKey(record.getOccurredAt(), periodUnit);
                        log.debug("==============>groupByTimeKey timeKey = {}", timeKey);
                        return timeKey;
                    }
                )
            );
    }

}
