package com.wiinvent.gami.streams.repositories;

import com.wiinvent.gami.streams.entities.Task;
import com.wiinvent.gami.streams.services.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DefaultTaskRepository extends BaseTaskRepository{

  private final ITaskService taskService;

  private long currentCheckpoint = 0;

  public DefaultTaskRepository(ITaskService taskService) {
    this.taskService = taskService;
  }

  @Override
  protected List<Task> loadTasks() {
    return List.of();
  }
}
