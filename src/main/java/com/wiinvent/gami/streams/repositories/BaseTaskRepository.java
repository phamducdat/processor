package com.wiinvent.gami.streams.repositories;

import com.wiinvent.gami.avro.EventLog;
import com.wiinvent.gami.streams.entities.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  protected abstract void onTasksLoaded(List<Task> tasks);

  protected abstract List<Task> loadTasks();

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
