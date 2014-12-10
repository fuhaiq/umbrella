package com.umbrella;

import java.util.Map;

import com.umbrella.beanstalkd.BeanstalkdConfig;
import com.umbrella.kernel.KernelConfig;
import com.umbrella.mail.MailConfig;
import com.umbrella.redis.JedisConfig;
import com.umbrella.service.ServiceConfig;

public class UmbrellaConfig {
	public BeanstalkdConfig getBeanstalkd() {
		return beanstalkd;
	}
	public void setBeanstalkd(BeanstalkdConfig beanstalkd) {
		this.beanstalkd = beanstalkd;
	}
	public KernelConfig getKernel() {
		return kernel;
	}
	public void setKernel(KernelConfig kernel) {
		this.kernel = kernel;
	}
	public MailConfig getMail() {
		return mail;
	}
	public void setMail(MailConfig mail) {
		this.mail = mail;
	}
	public JedisConfig getRedis() {
		return redis;
	}
	public void setRedis(JedisConfig redis) {
		this.redis = redis;
	}
	public Map<String, ServiceConfig> getService() {
		return service;
	}
	public void setService(Map<String, ServiceConfig> service) {
		this.service = service;
	}
	private BeanstalkdConfig beanstalkd;
	private KernelConfig kernel;
	private MailConfig mail;
	private JedisConfig redis;
	private Map<String, ServiceConfig> service;
}
