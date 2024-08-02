package com.wiinvent.gami.streams.processors;

import lombok.Data;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Data
public class Topics {

  @Value("${spring.kafka.topics.names.event-log}")
  private String eventLogTopic;

  @Value("${spring.kafka.topics.names.task-completion}")
  private String taskCompletionTopic;

  @Value("${spring.kafka.topics.config.num-partitions}")
  private int numPartitions;

  @Value("${spring.kafka.topics.config.replication-factor}")
  private short replicationFactor;

  @Value("${spring.kafka.topics.config.retention-ms}")
  private String retentionMs;

  @Bean
  public NewTopic eventLogTopic() {
    NewTopic topic = new NewTopic(eventLogTopic, numPartitions, replicationFactor);
    topic.configs(getConfigs());

    return topic;
  }


  private Map<String, String> getConfigs() {
    Map<String, String> configs = new HashMap<>();
    configs.put(TopicConfig.RETENTION_MS_CONFIG, retentionMs);

    return configs;
  }
}
