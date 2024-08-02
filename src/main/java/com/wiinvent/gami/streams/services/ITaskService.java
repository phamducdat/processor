package com.wiinvent.gami.streams.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "taskService")
public interface ITaskService {

  @GetMapping("/tasks")
  TaskLis
}
