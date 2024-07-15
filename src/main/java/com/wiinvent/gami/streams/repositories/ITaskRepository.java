package com.wiinvent.gami.streams.repositories;

import com.wiinvent.gami.avro.EventLog;
import com.wiinvent.gami.streams.entities.Task;

import java.util.List;

public interface ITaskRepository {

  List<Task> findByEvent(EventLog eventLog);

  Task findById(long taskId);
}
