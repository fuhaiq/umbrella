"use strict";

var plugin = {},
	http = require('http'),
	jsdom = require("jsdom"),
	fivebeans = require('fivebeans'),
	string = require('string'),
    fs = require('fs-extra'),
    jquery = fs.readFileSync("./node_modules/nodebb-plugin-kernel/jquery-2.1.4.min.js", "utf-8"),
	async = module.parent.require('async'),
	topics = module.parent.require('./topics'),
	plugins = module.parent.require('./plugins'),
    db = module.parent.require('./database'),
    nconf = module.parent.require('nconf'),
    winston = module.parent.require('winston'),
	posts = module.parent.require('./posts'),
    redis = require("redis"),
    io = module.parent.require('./socket.io');

var emit = function (post, callback) {
    posts.getTopicFields(post.pid, ['mainPid', 'cid', 'tid'], function (err, topic) {
        if(err) {
            return callback(err);
        }
        if(topic.mainPid == post.pid) {
            var message = {status: post.status, tid: topic.tid};
            io.in('category_' + topic.cid).emit('kernel:topic', message);
            io.in('recent_topics').emit('kernel:topic', message);
            io.in('popular_topics').emit('kernel:topic', message);
            io.in('unread_topics').emit('kernel:topic', message);
        }
        message = {status: post.status, pid: post.pid};
        io.in('topic_' + topic.tid).emit('kernel:post', message);
        return callback(null, null);
    });
};

plugin.http = {};

plugin.http.get = function(req, res, next) {
	return res.render('kernel', {});
};

plugin.http.post = function(req, res, next) {
	var content = req.body.content;
	if(!content) {
		return res.json({success: false, msg: '没有脚本可以运行', type: 'info'})
	}
	content = JSON.parse(content);
	var kernel = JSON.stringify({dir: nconf.get('imgDir'), scripts:content});

    var options = {
        path: '/evaluate',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
            'Content-Length': Buffer.byteLength(kernel, 'utf8')
        }
    };

    var request = http.request(options, function(response) {
        response.setEncoding('utf8');
        response.on('data', function(chunk) {
            switch(response.statusCode) {
                case 200 :
                    res.json({success: true, result: chunk});
                    break;
                case 502 :
                    res.json({success: false, msg: '计算服务目前不可用', type: 'danger'});
                    break;
                case 500 :
                    var act = JSON.parse(chunk);
                    if(string(act.exception).contains('java.util.NoSuchElementException')) {
                        res.json({success: false, msg: '目前没有空余的计算内核,请稍后再试', type: 'info'});
                        break;
                    } else if(string(act.exception).contains('java.util.concurrent.TimeoutException')) {
                        res.json({success: false, msg: '这个计算太耗时了,算不出来啊', type: 'info'});
                        break;
                    } else {
                        res.json({success: false, msg: '计算服务错误,我们会尽快解决', type: 'danger'});
                        break;
                    }
                default :
                    res.json({success: false, msg: '计算服务未知错误', type: 'danger'});
            }
        });
    });
    request.write(kernel);
    request.end();
};

plugin.init = function(data, callback) {

	data.router.get('/api/kernel', plugin.http.get);

	data.router.post('/kernel', data.middleware.applyCSRF, plugin.http.post);

	callback();
};

plugin.topic = {};

var populateStatus = function (status, topicOrPost) {
    switch(status) {
        case 1 :
            topicOrPost.waiting = true;
            break;
        case 2 :
            topicOrPost.evaluate = true;
            break;
        case 3 :
            topicOrPost.finished = true;
            break;
        case -1 :
            topicOrPost.error = true;
            break;
        case -2 :
            topicOrPost.aborted = true;
            break;
    }
    return topicOrPost;
}

plugin.topic.list = function(data, callback) {
	var topic_list = data.topics;
	async.map(topic_list, function(topic, next) {
		topics.getMainPost(topic.tid, topic.uid, function (err, post){
			if(err) {
				return next(err);
			}
            topic = populateStatus(post.status, topic);
			return next(null, topic);
		});
	}, function(err) {
		return callback(err, data);
	});
};

plugin.topic.get= function(data, callback) {
	var topic = data.topic;
	async.map(topic.posts, function (post, next) {
        post = populateStatus(post.status, post);
		if(post.result && post.result.length > 0) {
            jsdom.env({
                html: post.content,
                src: [jquery],
                done: function (err, window) {
                    if(err) {
                        return callback(err);
                    }
                    var codes = window.$("code[class='language-mma']");
                    for(var i = 0; i < post.result.length; i++) {
                        if(post.result[i].type == 'return' || post.result[i].type == 'text') {
                            window.$(codes[post.result[i].index]).after('<div class="kernel result alert alert-success" role="alert">'+post.result[i].data+'</div>');
                        } else if(post.result[i].type == 'error') {
                            window.$(codes[post.result[i].index]).after('<div class="kernel result alert alert-danger" role="alert">'+post.result[i].data+'</div>');
                        } else if(post.result[i].type == 'abort') {
                            window.$(codes[post.result[i].index]).after('<div class="kernel result alert alert-warning" role="alert">运行超时</div>');
                        } else if(post.result[i].type == 'image') {
                            window.$(codes[post.result[i].index]).after("<img class='kernel result' src='/kernel/post/"+post.pid+"/"+post.result[i].data+"'></img>");
                        }
                    }
                    var html = window.document.documentElement.outerHTML;
                    window.close();
                    html = string(html).replaceAll('<html><head></head><body>', '').s;
                    html = string(html).replaceAll('<script class="jsdom" src="http://www.wiseker.com/vendor/jquery/js/jquery.js"></script></body></html>', '').s;
                    post.content = html;
                    return next(null);
                }
            });
		} else {
			return next(null);
		}
	}, function (err){
		return callback(err, data);
	});
};

plugin.post = {};

plugin.post.filterEdit = function(postData, callback) {
    callback = callback || function() {};
    var client = redis.createClient(nconf.get('redis:port'), nconf.get('redis:host'));
    client.exists('post:lock:' + postData.post.pid, function (err, reply) {
        client.quit();
        if(err) {
            return callback(err);
        }
        if(reply == 1) {
            return callback(new Error('帖子正在计算,暂时不能修改.'));
        }
        return callback(null, postData);
    });
};

plugin.post.filterDelete = function(pid, callback) {
    callback = callback || function() {};
    var client = redis.createClient(nconf.get('redis:port'), nconf.get('redis:host'));
    client.exists('post:lock:' + pid, function (err, reply) {
        client.quit();
        if(err) {
            return callback(err);
        }
        if(reply == 1) {
            return callback(new Error('帖子正在计算,暂时不能删除.'));
        }
        return callback(null, pid);
    });
};

plugin.post.edit = function(post, callback) {
	callback = callback || function() {};
    db.deleteObjectField('post:' + post.pid, 'result', function (err) {
        if(err) {
            return callback(err);
        }
        async.waterfall([
            function (callback) {
                plugins.fireHook('filter:parse.raw', post.content, callback);
            },
            function (html, callback) {
                jsdom.env({
                    html: html,
                    src: [jquery],
                    done: callback
                });
            },
            function (window, callback) {
                var codes = window.$("code[class='language-mma']");
                window.close();
                return callback(null, codes);
            },
            function (codes, callback) {
                posts.setPostField(post.pid, 'status', (codes && codes.length > 0) ? 1 : 0, function (err) {
                    if(err) {
                        return callback(err)
                    }
                    post.status = (codes && codes.length > 0) ? 1 : 0;
                    emit(post, function (err) {
                        if(err) {
                            return callback(err)
                        }
                        var beans = new fivebeans.client(nconf.get('beanstalkd:host'), nconf.get('beanstalkd:port'));
                        beans.on('connect', function() {
                            beans.use('kernel', function (err) {
                                if (err) {
                                    return callback(err)
                                }
                                beans.put(Math.pow(2, 32), 0, 120, JSON.stringify({
                                    pid: post.pid,
                                    action: 'update'
                                }), function (err, jobid) {
                                    beans.end();
                                    return callback(err, jobid);
                                });
                            });
                        }).on('error', callback).connect();
                    });
                });
            }
        ], callback);//end of waterfall
    });
};

plugin.post.save = function(post, callback) {
    callback = callback || function() {};

    async.waterfall([
        function (callback) {
            plugins.fireHook('filter:parse.raw', post.content, callback);
        },
        function (html, callback) {
            jsdom.env({
                html: html,
                src: [jquery],
                done: callback
            });
        },
        function (window, callback) {
            var codes = window.$("code[class='language-mma']");
            window.close();
            return callback(null, codes);
        },
        function (codes, callback) {
            var send = false;
            if (codes && codes.length > 0) {
                send = true;
            }
            posts.setPostField(post.pid, 'status', (send) ? 1 : 0, function (err) {
                post.status = (send) ? 1 : 0;
                return callback(err, send);
            });
        }
    ], function (err, send) {
        if(err) {
            return callback(err);
        }
        if(send) {
            emit(post, function (err) {
                if(err) {
                    return callback(err);
                }
                var beans = new fivebeans.client(nconf.get('beanstalkd:host'), nconf.get('beanstalkd:port'));
                beans.on('connect', function() {
                    beans.use('kernel', function (err) {
                        if (err) {
                            return callback(err)
                        }
                        beans.put(Math.pow(2, 32), 0, 120, JSON.stringify({
                            pid: post.pid,
                            action: 'create'
                        }), function (err, jobid) {
                            beans.end();
                            return callback(err, jobid);
                        });
                    });
                }).on('error', callback).connect();
            })

        } else {
            return callback(null, null);
        }
    });
};

plugin.post.purge = function(pid, callback) {
	callback = callback || function() {};
	var beans = new fivebeans.client(nconf.get('beanstalkd:host'), nconf.get('beanstalkd:port'));
    beans.on('connect', function() {
        beans.use('kernel', function (err) {
            if (err) {
                return callback(err)
            }
            beans.put(Math.pow(2, 32), 0, 120, JSON.stringify({
                pid: pid,
                action: 'purge'
            }), function (err, jobid) {
                beans.end();
                return callback(err, jobid);
            });
        });
    }).on('error', callback).connect();
};


plugin.notification = function(data, callback) {
	callback = callback || function() {};
	var uids = data.uids,
		notification = data.notification;
	console.log(notification);
	return callback(null,null);
};

module.exports = plugin;
