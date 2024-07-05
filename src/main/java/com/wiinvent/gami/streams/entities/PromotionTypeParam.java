package com.wiinvent.gami.streams.entities;

import lombok.Data;

@Data
public class PromotionTypeParam {
  private String name;
  private DataType dataType;
  private Operator operator;
  private String value;


  public enum DataType {
    STRING
  }

  public enum Operator {
    IN_SET
  }

  public String getValue() {
    return value.replaceAll("\\s", "").trim();
  }
}
