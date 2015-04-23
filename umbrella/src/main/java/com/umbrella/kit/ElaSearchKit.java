package com.umbrella.kit;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.ibatis.session.SqlSessionManager;
import org.jsoup.Jsoup;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.umbrella.UmbrellaConfig;

public class ElaSearchKit {
	
	@Inject private UmbrellaConfig umbrella;
	
	@Inject private SqlSessionManager manager;
	
	public void modifyTag(int tagId, boolean created) throws SQLException, ClientProtocolException, IOException {
		String host = umbrella.getElasearch().getHost();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String address = created ? host + "/tag/" + tagId + "/_create" : host + "/tag/" + tagId;
		HttpPut put = new HttpPut(address);
		JSONObject tag = manager.selectOne("tag.select", tagId);
		String name = tag.getString("name");
		String description = tag.getString("description");
		description = Jsoup.parse(description).text();
		JSONObject entity = new JSONObject();
		entity.put("title", name);
		entity.put("content", description);
		put.setEntity(new StringEntity(entity.toJSONString(), Consts.UTF_8));
		CloseableHttpResponse res = httpclient.execute(put);
		try {
			int status = res.getStatusLine().getStatusCode();
			if(created) {
				if(status != HttpStatus.SC_CREATED) {
					throw new IllegalStateException("ElasticSearch返回码[" + status + "] 创建标签索引失败,可能是标签索引已经存在!");
				}
			}else{
				if(status != HttpStatus.SC_OK) {
					throw new IllegalStateException("ElasticSearch返回码[" + status + "] 更新标签索引失败,可能是标签索引不存在!");
				}
			}
			HttpEntity back = res.getEntity();
		    EntityUtils.consume(back);
        } finally {
            res.close();
        }
	}
	
	public void modifyReply(int replyId, boolean created) throws SQLException, ClientProtocolException, IOException {
		String host = umbrella.getElasearch().getHost();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String address = created ? host + "/reply/" + replyId + "/_create" : host + "/reply/" + replyId;
		HttpPut put = new HttpPut(address);
		JSONObject reply = manager.selectOne("reply.select", replyId);
		JSONObject topic = manager.selectOne("topic.select", reply.getInteger("topicid"));
		String title = topic.getString("title");
		String html = reply.getString("html");
		html = Jsoup.parse(html).text();
		JSONObject entity = new JSONObject();
		entity.put("title", title);
		entity.put("content", html);
		entity.put("topicid", topic.getInteger("id"));
		put.setEntity(new StringEntity(entity.toJSONString(), Consts.UTF_8));
		CloseableHttpResponse res = httpclient.execute(put);
		try {
			int status = res.getStatusLine().getStatusCode();
			if(created) {
				if(status != HttpStatus.SC_CREATED) {
					throw new IllegalStateException("ElasticSearch返回码[" + status + "] 创建回复索引失败,可能是回复索引已经存在!");
				}
			}else{
				if(status != HttpStatus.SC_OK) {
					throw new IllegalStateException("ElasticSearch返回码[" + status + "] 更新回复索引失败,可能是回复索引不存在!");
				}
			}
			HttpEntity back = res.getEntity();
		    EntityUtils.consume(back);
        } finally {
            res.close();
        }
	}
	
	public void modifyTopic(int topicId, boolean created) throws SQLException, ClientProtocolException, IOException {
		String host = umbrella.getElasearch().getHost();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String address = created ? host + "/topic/" + topicId + "/_create" : host + "/topic/" + topicId;
		HttpPut put = new HttpPut(address);
		JSONObject topic = manager.selectOne("topic.select", topicId);
		String title = topic.getString("title");
		String html = topic.getString("html");
		if(Strings.isNullOrEmpty(html)) {
			JSONObject entity = new JSONObject();
			entity.put("title", title);
			put.setEntity(new StringEntity(entity.toJSONString(), Consts.UTF_8));
		} else {
			html = Jsoup.parse(html).text();
			JSONObject entity = new JSONObject();
			entity.put("title", title);
			entity.put("content", html);
			put.setEntity(new StringEntity(entity.toJSONString(), Consts.UTF_8));
		}
		CloseableHttpResponse res = httpclient.execute(put);
		try {
			int status = res.getStatusLine().getStatusCode();
			if(created) {
				if(status != HttpStatus.SC_CREATED) {
					throw new IllegalStateException("ElasticSearch返回码[" + status + "] 创建话题索引失败,可能是话题索引已经存在!");
				}
			}else{
				if(status != HttpStatus.SC_OK) {
					throw new IllegalStateException("ElasticSearch返回码[" + status + "] 更新话题索引失败,可能是话题索引不存在!");
				}
			}
			HttpEntity back = res.getEntity();
		    EntityUtils.consume(back);
        } finally {
            res.close();
        }
	}
	
	public void delete(String prefix, int indexId) throws ClientProtocolException, IOException {
		String host = umbrella.getElasearch().getHost();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String address = host + prefix + indexId;
		HttpDelete delete = new HttpDelete(address);
		CloseableHttpResponse res = httpclient.execute(delete);
		try {
			int status = res.getStatusLine().getStatusCode();
			if(status != HttpStatus.SC_OK) {
				throw new IllegalStateException("ElasticSearch返回码[" + status + "] 删除索引失败,可能是索引已经不存在!");
			}
			HttpEntity back = res.getEntity();
		    EntityUtils.consume(back);
        } finally {
            res.close();
        }
	}
}
