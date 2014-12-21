package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.umbrella.beanstalkd.BeanstalkdJob;

public class BeanstalkdSearchService extends BeanstalkdService {

	public BeanstalkdSearchService(String host, int port) {
		super("search", LogManager.getLogger("beanstalkd-search-service"));
		this.host = host;
		this.port = port;
	}

	private final String host;
	
	private final int port;
	
	private CloseableHttpClient httpClient;
	
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

	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		JSONObject json = JSON.parseObject(job.getData());
		String url = checkNotNull(json.getString("url"), "no url in search job " + job.getId());
		String type = checkNotNull(json.getString("type"), "no type in search job " + job.getId());
		HttpRequest request = null;
		switch (type) {
		case "d":
			request = new HttpDelete();
			break;
		default:
			request = entityRequest(job.getId(), type, json.getString("data"));
		}
		try (CloseableHttpResponse response = httpClient.execute(new HttpHost(host + url, port), request)) {
			StatusLine status = response.getStatusLine();
			if(200 != status.getStatusCode()) {
				throw new HttpException("bad status code " + status.getStatusCode() + " >>" + status.getReasonPhrase());
			}
		}
	}
}
