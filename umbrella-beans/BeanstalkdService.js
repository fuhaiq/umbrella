'use strict';

var MarkdownIt = require('markdown-it'),
    md = new MarkdownIt({
		'langPrefix': 'language-'
	}),
	fs = require('fs-extra'),
	jquery = fs.readFileSync("./jquery-2.1.4.min.js", "utf-8"),
	jsdom = require("jsdom"),
	async = require('async'),
	http = require('http');

var BeanstalkdService = function(db, redis) {

	var emit = function (pid, callback) {
	    async.waterfall([
	        function (callback) {
	            db.collection("objects").findOne({_key:'post:' + pid}, callback);
	        },
	        function (post, callback) {
	            db.collection("objects").findOne({_key:'topic:' + post.tid}, function (err, topic) {
	                return callback(err, post, topic);
	            });
	        },
	        function (post, topic, callback) {
	        	var message = {status: post.status, tid: topic.tid};
	        	if(post.pid == topic.mainPid) {
		            io.to('category_' + topic.cid).emit('kernel:topic', message);
		            io.to('recent_topics').emit('kernel:topic', message);
		            io.to('popular_topics').emit('kernel:topic', message);
		            io.to('unread_topics').emit('kernel:topic', message);
	        	}
	        	message = {status: post.status, pid: pid};
	        	io.to('topic_' + topic.tid).emit('kernel:post', message);
	        	return callback(null, null);
	        }
	    ], callback);
	};

	var kernel = function (post, pid, dir, url, callback) {
		async.waterfall([
			function (callback) {
				var html = md.render(post.content);
				jsdom.env({
		            html: html,
		            src: [jquery],
		            done: function (err, window) {
		            	if(err) {
		            		return callback(err);
		            	}
		                var codes = window.$("code[class='language-mma']");
		                if(codes.length < 1) {
		                	window.close();
		                	return callback('回复[post:'+post.pid+']内容找不到执行脚本');
		                }
		                var scripts = []
		                for(var i = 0; i < codes.length; i++) {
		                	scripts.push(window.$(codes[i]).text())
		                }
		                window.close();
		                return callback(null, scripts);
		            }
		        });
			},
			function (scripts, callback) {
				db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:2}}, function (err) {
					if(err) {
						return callback(err);
					}
					emit(pid, function () {
						fs.mkdirsSync(dir + pid);
						var kernel = JSON.stringify({dir: dir + pid + '/', scripts:scripts});
						var options = {
					        path: url,
					        method: 'POST',
					        headers: {
					            'Content-Type': 'application/json;charset=UTF-8',
					            'Content-Length': Buffer.byteLength(kernel, 'utf8')
					        }
					    };
					    var request = http.request(options, function(response) {
					        response.setEncoding('utf8');
					        response.on('data', function(chunk) {
					        	var act = JSON.parse(chunk);
					            switch(response.statusCode) {
					                case 200 :
					                	var lastData = act[act.length - 1];
							        	var status = 3;
							        	if(lastData.type == 'error') {
							        		status = -1;
							        	} else if(lastData.type == 'abort') {
							        		status = -2;
							        	}
					                	db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:status, result: act}}, function (err) {
					                		return callback(err, false);
					                	});
					                    break;
					                case 502 :
					                	db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:1}}, function (err) {
					                		return callback(err, true);
					                	});
					                    break;
					                case 500 :
					                    if(string(act.exception).contains('java.util.NoSuchElementException')) {
					                    	db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:1}}, function (err) {
						                		return callback(err, true);
						                	});
					                        break;
					                    } else if(string(act.exception).contains('java.util.concurrent.TimeoutException')) {
					                        db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:-2}}, function (err) {
						                		return callback(err, false);
						                	});
					                        break;
					                    } else {
					                    	db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:1}}, function (err) {
						                		return callback(err, true);
						                	});
					                        break;
					                    }
					                default :
					                	db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:1}}, function (err) {
					                		return callback(err, true);
					                	});
					            }
					        });
					    });
					    request.write(kernel);
					    request.end();
					})
				});
			}
		], function (err, needRelease) {
			if(err) {
				db.collection("objects").updateOne({_key:'post:' + pid}, {$set:{status:1}}, function () {
            		emit(pid, function () {
            			return callback(err);
					});
            	});
			} else {
				emit(pid, function () {
					return callback(null, needRelease);
				})
			}
		}); // end of waterfall
	};

	var db = db;

	var io = require('socket.io-emitter')(redis);

	this.update = function (jobData, callback) {
		var dir = jobData.dir,
			pid = jobData.pid,
			url = jobData.url;
		db.collection("objects").findOne({_key:'post:' + pid}, function (err, post) {
			if(err) {
				return callback(err);
			}
			if(!post) {
				return callback('回复[post:'+pid+']不存在');
			}
			if(post.status == 0) {
				fs.removeSync(dir + pid);
				return emit(pid, function () {
					return callback(null, false);
				});
			} else if(post.status == 1) {
				fs.removeSync(dir + pid);
				return kernel(post, pid, dir, url, callback);
			} else {
				return callback('回复[post:'+post.pid+']状态错误['+post.status+'], 期望值: 1或0');
			}
		});
	};

	this.purge = function (jobData, callback) {
		var dir = jobData.dir,
			pid = jobData.pid;
		fs.removeSync(dir + pid);
		return callback(null, false);
	};

	this.create = function (jobData, callback) {
		var dir = jobData.dir,
			pid = jobData.pid,
			url = jobData.url;
		db.collection("objects").findOne({_key:'post:' + pid}, function (err, post) {
			if(err) {
				return callback(err);
			}
			if(!post) {
				return callback('回复[post:'+pid+']不存在');
			}
			if(post.status != 1) {
				return callback('回复[post:'+post.pid+']状态错误['+post.status+'], 期望值: 1');
			}
			return kernel(post, pid, dir, url, callback);
		});
	};

}

module.exports = BeanstalkdService;