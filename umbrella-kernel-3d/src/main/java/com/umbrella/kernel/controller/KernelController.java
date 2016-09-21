package com.umbrella.kernel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.umbrella.kernel.link.Kernel;

@RestController
public class KernelController {
	
	@Autowired
	private Kernel kernel;
	
	@RequestMapping(method = RequestMethod.POST, value = "/", produces = "application/json;charset=UTF-8")
	public @ResponseBody String evaluate(@RequestBody String body) throws Exception{
		return kernel.evaluate(JSON.parseObject(body)).toJSONString();
	}
	
}
