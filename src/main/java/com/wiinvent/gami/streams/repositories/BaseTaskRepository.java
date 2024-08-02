package com.wiinvent.gami.streams.repositories;

import com.wiinvent.gami.avro.EventLog;
import com.wiinvent.gami.streams.entities.Quest;
import com.wiinvent.gami.streams.entities.QuestType;
import com.wiinvent.gami.streams.entities.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseTaskRepository implements ITaskRepository {

  protected Set<Task> tasks = Collections.emptySet();

  // TODO: AtomicReference
  protected Map<String, List<Task>> tasksByEvent = Collections.emptyMap();
  protected Map<Long, Task> tasksById = Collections.emptyMap();

  public void doLoadTasks() {
    List<Task> tasks = loadTasks();
    onTasksLoaded(tasks);
  }

  protected abstract List<Task> loadTasks();

  protected void onTasksLoaded(List<Task> tasks) {
    updateTasks(tasks);
    updateTasksByEvent();
    updateTasksById();
  }

  private void updateTasksById() {
    Map<Long, Task> newTasksById = new HashMap<>();

    tasks.forEach(
        task -> {
          newTasksById.put(task.getId(), task);
        });

    tasksById = newTasksById;
  }

  private void updateTasks(List<Task> taskList) {
    Set<Task> newTasks = new HashSet<>(taskList);
    newTasks.addAll(this.tasks);

    this.tasks = newTasks;
    log.info("{} task(s) loaded", this.tasks.size());
  }

  private void updateTasksByEvent() {
    Map<String, List<Task>> newTasksByEvent = new HashMap<>();

    tasks.forEach(
        task -> {
          List<Quest> quests = ObjectUtils.defaultIfNull(task.getQuests(), Collections.emptyList());
          quests.forEach(
              quest -> {
                QuestType questType = quest.getType();
                if (!newTasksByEvent.containsKey(questType.getExternalId())) {
                  newTasksByEvent.put(questType.getExternalId(), new ArrayList<>());
                }
              }
          );
        }
    );

    tasksByEvent = newTasksByEvent;
  }

  @Override
  public List<Task> findByEvent(EventLog eventLog) {
    List<Task> tasks = tasksByEvent.getOrDefault(eventLog.getType(), Collections.emptyList());
    return tasks.stream().filter(Task::isEffective).collect(Collectors.toList());
  }

  @Override
  public Task findById(long taskId) {
    if (tasksById.containsKey(taskId)) {
      return tasksById.get(taskId);
    } else {
      throw new TaskNotFoundException("No task with id " + taskId);
    }
  }
}
