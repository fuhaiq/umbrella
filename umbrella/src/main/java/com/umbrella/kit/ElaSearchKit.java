package com.umbrella.kit;

import java.sql.SQLException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.UmbrellaConfig;

public class ElaSearchKit {
	
	private final Logger LOG = LogManager.getLogger("ElasearchKit");
	
	@Inject private UmbrellaConfig umbrella;
	
	@Inject private SqlSessionManager manager;

	public void create(int topicId) throws SQLException {
		String host = umbrella.getElasearch().getHost();
		JSONObject topic = manager.selectOne("topic.select", topicId);
		int status = topic.getIntValue("status");
		if(status == 3) {
			String html = topic.getString("html");
			html = Jsoup.parse(html).text();
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPut put = new HttpPut(host + "/topic/" + topicId + "/_create");
			put.setEntity(null);
		} else {
			String html = topic.getString("html");
			html = Jsoup.parse(html).text();
			String title = topic.getString("title");
			
		}
	}
}
