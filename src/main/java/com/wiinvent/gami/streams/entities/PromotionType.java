package com.wiinvent.gami.streams.entities;

import lombok.Data;

import java.util.List;

@Data
public class PromotionType {
  private int id;
  private String name;
  private String code;
  private List<PromotionTypeParam> params;
}
