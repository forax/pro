package com.github.forax.pro.api.helper;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface ProConf {
  public String loglevel();
  public void loglevel(String loglevel);
}
