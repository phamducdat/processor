package com.wiinvent.gami.streams.processors;

import com.wiinvent.gami.avro.EventLog;
import com.wiinvent.gami.avro.QuestState;
import com.wiinvent.gami.avro.TaskEvent;
import com.wiinvent.gami.avro.TaskState;
import com.wiinvent.gami.streams.common.DateTimeUtils;
import com.wiinvent.gami.streams.entities.Quest;
import com.wiinvent.gami.streams.entities.Task;
import com.wiinvent.gami.streams.repositories.ITaskRepository;
import com.wiinvent.gami.streams.repositories.TaskNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

                    QuestState questState = taskState
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

                  }
              );
            }
        )
  }


  private void adjustWindowTime(Task task, TaskState taskState) {
    // TODO
  }


}
