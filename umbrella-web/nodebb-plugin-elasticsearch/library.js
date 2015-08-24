"use strict";

var plugin = {},
	string = require('string'),
	elasticsearch = require('elasticsearch'),
	jsdom = require("jsdom"),
	client = null,
	async = module.parent.require('async'),
	_ = module.parent.require('underscore'),
	meta = module.parent.require('./meta'),
	topics = module.parent.require('./topics'),
	posts = module.parent.require('./posts'),
	categories = module.parent.require('./categories'),
	plugins = module.parent.require('./plugins'),
	winston = module.parent.require('winston');

var isDeleted = function(pid, callback) {
	topics.getTopicFieldByPid('deleted', pid, function(err, deleted) {
		callback(err, parseInt(deleted, 10) === 1);
	});
};

var insert = function(type, id, value, cid, uid, callback) {
	var query = {
		index: 'umbrella',
		type: type,
		id: id,
		body: {
			deleted: false,
			cid: parseInt(cid, 10),
			uid: parseInt(uid, 10)
		}
    }
    if(type == 'topic') {
    	query.body.title = value;
    } else {
    	query.body.content = value;
    }
	return client.create(query, callback);
};

var update = function(type, id, value, callback) {
	var query = {
		index: 'umbrella',
		type: type,
		id: id,
		body: {
			doc: null
		}
	};
	if(type == 'topic') {
		query.body.doc = {title: value};
	} else {
		query.body.doc = {content: value};
	}
	return client.update(query, callback);
};

var remove = function(type, id, callback) {
	return client.update(
	{
		index: 'umbrella',
		type: type,
		id: id,
		body: {
			doc: {
				deleted: true
			}
		}
	}, callback);
};

var restore = function(type, id, callback) {
	return client.update(
	{
		index: 'umbrella',
		type: type,
		id: id,
		body: {
			doc: {
				deleted: false
			}
		}
	}, callback);
};

var purge = function(type, id, callback) {
	return client.delete(
	{
		index: 'umbrella',
		type: type,
		id: id
	}, callback)
};

plugin.init = function(data, callback) {
	client = new elasticsearch.Client({
		host: 'localhost:9200',
		log: 'error'
	});
	callback();
};

plugin.searchTopic = function(quert, callback) {
	callback = callback || function() {};
	return callback(null);
}

plugin.search = function(data, callback) {
	var uid = [],
		cid = [];

	if(data.uid && data.uid.length > 0) {
		for(var index = 0; index < data.uid.length; index++) {
			if(string(data.uid[index]).isNumeric()) {
				uid.push(data.uid[index]);	
			} else {
				uid.push(-99);
			}
		}
	}

	if(data.cid && data.cid.length > 0) {
		for(var index = 0; index < data.cid.length; index++) {
			if(string(data.cid[index]).isNumeric()) {
				cid.push(data.cid[index]);	
			}
		}
	}

	var query = {
		index: 'umbrella',
		type: data.index,
		body: {
			query: {
				bool: {
					must: [
						{
							term: {
								deleted: false
							}
						}
					],
					should: {
						match: null
					},
					minimum_should_match: 1
				}
			}
		}
	};

	if(uid.length > 0) {
		query.body.query.bool.must.push(
			{
               terms: {
                  uid: uid
               }
            }
		);
	}

	if(cid.length > 0) {
		query.body.query.bool.must.push(
			{
               terms: {
                  cid: cid
               }
            }
		);
	}

	if(data.index == 'topic') {
		query.body.query.bool.should.match = {
			title: data.content
		};
		
	} else if(data.index == 'post') {
		query.body.query.bool.should.match = {
			content: data.content
		};		
	} else {
		var msg = 'no this type of index ['+data.index+'] defined.'
		winston.error(msg);
		return callback(msg);
	}
	client.search(query, function(err, obj) {
    	if(err) {
    		winston.error(err);
    		return callback(err);
    	}
    	if(obj && obj.hits && obj.hits.hits && obj.hits.hits.length > 0) {
    		if(data.index == 'topic') {
    			var payload = obj.hits.hits.map(function(result) {
					return parseInt(result._id, 10);
				});
				return callback(null, payload);
    		} else {
    			async.map(obj.hits.hits, function (hit, callback){
    				isDeleted(hit._id, function (err, deleted) {
    					if(err) {
    						return callback(err);
    					}
    					if(deleted) {
    						return callback(null, null);
    					} else {
    						return callback(null, parseInt(hit._id, 10));	
    					}
    				});
    			}, callback);
    		}
    	} else {
    		return callback(null, []);
    	}
    });
};

plugin.topic = {};

plugin.topic.post = function(topic, callback) {
	callback = callback || function() {};
	return insert('topic', topic.tid, topic.title, topic.cid, topic.uid, callback);
};

plugin.topic.delete = function(topic, callback) {
	callback = callback || function() {};
	return remove('topic', topic.tid, callback);
};

plugin.topic.edit = function(topic, callback) {
	callback = callback || function() {};
	return update('topic', topic.tid, topic.title, callback);
};

plugin.topic.restore = function(topic, callback) {
	callback = callback || function() {};
	return restore('topic', topic.tid, callback);
};

plugin.topic.purge = function(tid, callback) {
	callback = callback || function() {};
	return purge('topic', tid, callback);
};

plugin.topic.move = function(info, callback) {
	callback = callback || function() {};
	var query = {
		body: [
			{ update: { _index: 'umbrella', _type: 'topic', _id: info.tid } },
			{ doc: { cid: parseInt(info.toCid, 10) } }
		]
	};
	topics.getPids(info.tid, function (err, pids) {
		if(err) {
			winston.error(err);
			return callback(err);
		}
		for(var index = 0; index < pids.length; index++) {
			query.body.push(
				{ update: { _index: 'umbrella', _type: 'post', _id: pids[index] } },
				{ doc: { cid: parseInt(info.toCid, 10) } }
			);
		}
		return client.bulk(query, callback);
	});
};

plugin.post= {};

plugin.post.save = function(post, callback) {
	callback = callback || function() {};
	plugins.fireHook('filter:parse.raw', post.content, function (err, html){
		if(err) {
			winston.error(err);
			return callback(err);
		}
		jsdom.env(
			html,
			['http://www.wiseker.com/vendor/jquery/js/jquery.js'],
			function (err, window) {
				if (err) {
					window.close();
					winston.error(err);
					return callback(err);
				}
				var text = window.$(html).text();
				window.close();
				return insert('post', post.pid, text, post.cid, post.uid, callback);
			});
	});
};

plugin.post.delete = function(pid, callback) {
	callback = callback || function() {};
	return remove('post', pid, callback);
};

plugin.post.edit = function(post, callback) {
	callback = callback || function() {};
	plugins.fireHook('filter:parse.raw', post.content, function (err, html){
		if(err) {
			winston.error(err);
			return callback(err);
		}
		jsdom.env(
			html,
			['http://www.wiseker.com/vendor/jquery/js/jquery.js'],
			function (err, window) {
				if (err) {
					window.close();
					winston.error(err);
					return callback(err);
				}
				var text = window.$(html).text();
				window.close();
				return update('post', post.pid, html, callback);
			});
	});
	
};

plugin.post.restore = function(post, callback) {
	callback = callback || function() {};
	return restore('post', post.pid, callback);
};

plugin.post.purge = function(pid, callback) {
	callback = callback || function() {};
	return purge('post', pid, callback);
};

plugin.post.move = function(info, callback) {
	callback = callback || function() {};
	topics.getTopicField(info.tid, 'cid', function (err, cid) {
		if(err) {
			winston.error(err);
			return callback(err);
		}
		return client.update(
		{
			index: 'umbrella',
			type: 'post',
			id: info.post.pid,
			body: {
				doc: {
					cid: parseInt(cid, 10)
				}
			}
		}, callback);
	});
};

plugin.morelikethis = function(topic, callback) {
	callback = callback || function() {};
	var query = 
	{
		index: 'umbrella',
		type: 'topic',
		body: {
			fields: [],
			query: {
				bool: {
					must: [
						{
							term: {
								deleted: false
							}
						}
					],
					should: {
						more_like_this: {
							fields: [
								"title"
							],
							docs: [
								{
									_index: "umbrella",
									_type: "topic",
									_id: parseInt(topic.tid, 10)
								}
							],
							min_term_freq: 1,
							max_query_terms: 10,
							min_doc_freq: 1
						}
					},
					minimum_should_match: 1
				}
			}
		}
	};

	client.search(query, function (err, res) {
		if(err) {
			winston.error(err);
			return callback(err);
		}
		if(res.hits.length == 0) {
			return callback(null, topic);
		} else {
			var tids = [];
			for(var index = 0; index < res.hits.hits.length; index++) {
				tids.push(parseInt(res.hits.hits[index]._id, 10));
			}

			topics.getTopicsData(tids, function (err, similar) {
				function mapFilter(array, field) {
					return array.map(function(topic) {
						return topic && topic[field] && topic[field].toString();
					}).filter(function(value, index, array) {
						return string(value).isNumeric() && array.indexOf(value) === index;
					});
				}
				if (err) {
					winston.error(err);
					return callback(err);
				}
				var cids = mapFilter(similar, 'cid');
				async.parallel({
					teasers: function(next) {
						topics.getTeasers(similar, next);
					},
					categories: function(next) {
						categories.getMultipleCategoryFields(cids, ['cid', 'name', 'slug', 'icon', 'bgColor', 'color', 'disabled'], next);
					}
				}, function (err, results) {
					if (err) {
						winston.error(err);
						return callback(err);
					}
					var categories = _.object(cids, results.categories);
					for (var i=0; i<similar.length; ++i) {
						if (similar[i]) {
							similar[i].category = categories[similar[i].cid];
							similar[i].teaser = results.teasers[i];
							similar[i].unreplied = parseInt(similar[i].postcount, 10) <= 1 && meta.config.teaserPost !== 'first';
						}
					}
					topic.similar = similar;
					return callback(null, topic);

				});
			});
		}
	});
}


module.exports = plugin;