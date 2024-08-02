package com.wiinvent.gami.streams.services;

import lombok.Data;

@Data
public class PagingMetadata {
  private int page;
  private int pageSize;
  private int count;
  private int totalElements;
  private int totalPages;
}
