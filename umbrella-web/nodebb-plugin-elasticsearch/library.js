"use strict";

var plugin = {},
	string = require('string'),
	elasticsearch = require('elasticsearch'),
	client = null,
	async = module.parent.require('async'),
	topics = module.parent.require('./topics'),
	posts = module.parent.require('./posts'),
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

plugin.topic.restore = function(topic, callback) {
	callback = callback || function() {};
	return restore('topic', topic.tid, callback);
};

plugin.topic.purge = function(tid, callback) {
	callback = callback || function() {};
	return purge('topic', tid, callback);
};

plugin.post= {};

plugin.post.save = function(post, callback) {
	callback = callback || function() {};
	return insert('post', post.pid, post.content, post.cid, post.uid, callback);
};

plugin.post.delete = function(pid, callback) {
	callback = callback || function() {};
	return remove('post', pid, callback);
};

plugin.post.restore = function(post, callback) {
	callback = callback || function() {};
	return restore('post', post.pid, callback);
};

plugin.post.purge = function(pid, callback) {
	callback = callback || function() {};
	return purge('post', pid, callback);
};


module.exports = plugin;