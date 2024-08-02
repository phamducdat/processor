package com.wiinvent.gami.streams.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "taskService")
public interface ITaskService {

  @GetMapping("/tasks")
  TaskListResponse getTasks(@RequestParam long checkPoint);
}
