package com.umbrella.service.beanstalkd.action;

import java.util.function.Consumer;

import com.alibaba.fastjson.JSONObject;

@FunctionalInterface
public interface DBAction extends Consumer<JSONObject>{}