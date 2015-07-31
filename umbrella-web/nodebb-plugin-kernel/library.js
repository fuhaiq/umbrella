"use strict";

var plugin = {},
	net = require('net'),
	winston = module.parent.require('winston'),
	async = module.parent.require('async'),
	topics = module.parent.require('./topics'),
	posts = module.parent.require('./posts');

plugin.get = function(req, res, next) {
	return res.render('kernel', {});
};

plugin.post = function(req, res, next) {
	var content = req.body.content;
	if(!content) {
		return res.json({success: false, msg: '没有脚本可以运行'})
	}
	var kernel = {id:'kernel', dir:'/home/wesker/git/umbrella-web/public/kernel/temp/', scripts:[content]};
	var conn = new net.Socket();
		conn.connect(8001, 'localhost', function() {
		conn.write(JSON.stringify(kernel));
	});
	conn.on('data', function(data) {
		conn.destroy();
		return res.json({success: true, data: new Buffer(data).toString()});
	});
	conn.on('error', function(err) {
		winston.error(err);
		return res.sendStatus(500);
	});
};

plugin.init = function(data, callback) {

	data.router.get('/api/kernel', plugin.get);

	data.router.post('/kernel', data.middleware.applyCSRF, plugin.post);

	callback();
};

plugin.topic = {};

plugin.getTopics = function(data, callback) {
	var topics = data.topics;

	async.map(topics, function(topic, next) {
		if(topic.status == 0) {
			topic.title = '<span class="finished"><i class="fa fa-check"></i> 等待运算</span> ' + topic.title;
		}
		return next(null, topic);
	}, function(err, topics) {
		return callback(err, data);
	});
};

plugin.topic.post = function(topic, callback) {

	callback = callback || function() {};

	topics.getMainPost(topic.tid, topic.uid, function(err, post){
		if(err) {
			winston.error(err);
			return callback(err);
		}
		if(!post) {
			err = 'main post is null';
			winston.error(err);
			return callback(err);
		}
		winston.info(post.content);
		topics.setTopicField(topic.tid, 'status', 0, callback);
	});
};


module.exports = plugin;