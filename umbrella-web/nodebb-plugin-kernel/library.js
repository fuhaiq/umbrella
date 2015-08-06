"use strict";

var plugin = {},
	net = require('net'),
	jsdom = require("jsdom"),
	fivebeans = require('fivebeans'),
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
		if(topic.status == 1) {
			topic.title = '<span class="waiting"><i class="fa fa-clock-o"></i> 等待运算</span> ' + topic.title;
		} else if(topic.status == 2) {
			topic.title = '<span class="evaluate"><i class="fa fa-play"></i> 正在计算</span> ' + topic.title;
		} else if(topic.status == 3) {
			topic.title = '<span class="finished"><i class="fa fa-check"></i> 计算完成</span> ' + topic.title;
		} else if(topic.status == -1) {
			topic.title = '<span class="error"><i class="fa fa-remove"></i> 语法错误</span> ' + topic.title;
		} else if(topic.status == -2) {
			topic.title = '<span class="aborted"><i class="fa fa-exclamation"></i> 计算超时</span> ' + topic.title;
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
		jsdom.env(
			post.content,
			['http://www.wiseker.com/vendor/jquery/js/jquery.js'],
			function (err, window) {
				if (err) {
					window.close();
					winston.error(err);
					return callback(err);
				}
				var codes = window.$("code[class='language-mma']");
				window.close();
				if (codes && codes.length > 0) {
					var beans = new fivebeans.client('localhost', 11300);
					beans.on('connect', function () {
						beans.use('kernel-topic', function (err) {
							if (err) {
								winston.error(err);
								return callback(err)
							}
							beans.put(Math.pow(2, 32), 0, 120, JSON.stringify({tid: topic.tid, mainpid: topic.mainPid, action: 'create'}), function (err, jobid) {
								beans.end();
								if(err) {
									winston.error(err);
									return callback(err);
								}
								topics.setTopicField(topic.tid, 'status', 1, callback);
							});
						});
				    }).on('error', function (err) {
				    	winston.error(err);
						return callback(err);
				    }).connect();

					
				} else {
					topics.setTopicField(topic.tid, 'status', 0, callback);
				}
			}
		);
	});
};


module.exports = plugin;