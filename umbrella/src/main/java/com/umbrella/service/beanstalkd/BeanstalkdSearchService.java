package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;

public class BeanstalkdSearchService extends AbstractExecutionThreadService {

	private final Logger LOG = LogManager.getLogger(BeanstalkdSearchService.class);
	
	private final String host;
	
	private final int port;
	
	private CloseableHttpClient httpClient;
	
	@Inject private ObjectPool<Beanstalkd> pool;
	
	private Beanstalkd bean;
	
	public BeanstalkdSearchService(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	@Override
	protected void startUp() throws Exception {
		httpClient = HttpClients.createDefault();
		bean = pool.borrowObject();
		bean.watch("search");
		LOG.info("beanstalkd search service starts");
	}

	@Override
	protected void shutDown() throws Exception {
		httpClient.close();
		pool.invalidateObject(bean);
		LOG.info("beanstalkd search service stops");
	}

	@Override
	protected void triggerShutdown() {
		bean.close();
	}
	
	@Override
	protected void run() throws Exception {
		while (isRunning()) {
			String job = bean.reserve();
			if(!Strings.isNullOrEmpty(job)) {
				BeanstalkdSearchJob searchJob = new BeanstalkdSearchJob(job);
				if(execute(searchJob)) {
					bean.delete(searchJob.getId());
				} else {
					//TODO release this job with higher priority without delay
				}
			}
		}
	}

	private boolean execute(BeanstalkdSearchJob searchJob) throws ClientProtocolException, UnsupportedEncodingException {
		JSONObject json = searchJob.getJson();
		String url = checkNotNull(json.getString("url"), "no url in search job " + searchJob.getId());
		String type = checkNotNull(json.getString("type"), "no type in search job " + searchJob.getId());
		HttpRequest request = null;
		switch (type) {
		case "d":
			request = new HttpDelete();
			break;
		default:
			request = entityRequest(searchJob.getId(), type, json.getString("data"));
		}
		try (CloseableHttpResponse response = httpClient.execute(new HttpHost(host + url, port), request)) {
			StatusLine status = response.getStatusLine();
			if(200 != status.getStatusCode()) {
				LOG.error("get " + status.getStatusCode() + " response code >>" + status.getReasonPhrase());
				return false;
			}
			return true;
		} catch (IOException e) {
			LOG.error("IO error when http call to elasticsearch >>", e);
			return false;
		}
	}
	
	private HttpEntityEnclosingRequest entityRequest(long id, String type, String data) throws UnsupportedEncodingException {
		checkNotNull(data, "no data in entity search job " + id);
		HttpEntityEnclosingRequest request = null;
		switch (type) {
		case "u":
			request = new HttpPut();
			break;
		case "p":
			request = new HttpPost();
			break;
		default:
			throw new IllegalStateException("undefined type [" + type + "] in search job " + id);
		}
		request.setEntity(new StringEntity(data));
		return request;
	}
}
