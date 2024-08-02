package com.wiinvent.gami.streams.services;

import com.wiinvent.gami.streams.entities.Task;
import lombok.Data;

import java.util.List;

@Data
public class TaskListResponse {
  private List<Task> tasks;
  private PagingMetadata metadata;

}
