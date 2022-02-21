package com.codergists.nio.files.model;

import lombok.ToString;

import java.io.Serializable;

@ToString
public class Session implements Serializable {

  private String id;
  private String name;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
