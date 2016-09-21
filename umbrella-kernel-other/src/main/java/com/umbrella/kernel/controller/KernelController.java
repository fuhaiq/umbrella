package com.umbrella.kernel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.umbrella.kernel.link.morse.Morse;

@RestController
public class KernelController {
	
	@Autowired
	private Morse morse;
	
	@RequestMapping(method = RequestMethod.POST, value = "/morse/encode", produces = "application/json;charset=UTF-8")
	public @ResponseBody String encode(@RequestBody String body) throws Exception{
		return morse.encode(JSON.parseObject(body)).toJSONString();
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/morse/decode", produces = "application/json;charset=UTF-8")
	public @ResponseBody String decode(@RequestBody String body) throws Exception{
		return morse.decode(JSON.parseObject(body)).toJSONString();
	}
	
}
