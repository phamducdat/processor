package com.wiinvent.gami.streams.processors;

import com.wiinvent.gami.streams.repositories.ITaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TaskCompletionStreamProcessor {
  @Autowired
  private ITaskRepository taskRepository;
}
