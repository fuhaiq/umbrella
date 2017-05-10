"use strict";

var plugin = {},
http = require('http'),
jsdom = require("jsdom"),
fivebeans = require('fivebeans'),
string = require('string'),
fs = require('fs-extra'),
jquery = fs.readFileSync("./node_modules/nodebb-plugin-kernel/static/jquery-2.1.4.min.js", "utf-8"),
async = module.parent.require('async'),
topics = module.parent.require('./topics'),
plugins = module.parent.require('./plugins'),
db = module.parent.require('./database'),
nconf = module.parent.require('nconf'),
winston = module.parent.require('winston'),
posts = module.parent.require('./posts'),
redis = require("redis"),
io = module.parent.require('./socket.io'),
uuid = require('uuid'),
helpers = module.parent.require('./routes/helpers');

var modifyPages = (next) => {
	async.parallel([
		next => {
			var file = './node_modules/nodebb-plugin-markdown/public/js/highlight.js'
			var highlight = fs.readFileSync(file, "utf-8")

			if(string(highlight).contains('hljs.registerLanguage("mathematica"')) {
				return next()
			}

			var append = fs.readFileSync('./node_modules/nodebb-plugin-kernel/static/ace/append_to_highlight.js', "utf-8")
			fs.outputFile(file, highlight + append, next)
		}
	], next)
}

var emit = (post, next) => {
	posts.getTopicFields(post.pid, ['mainPid', 'cid', 'tid'], (err, topic) => {
		if(err) {
			return next(err)
		}

		if(topic.mainPid != post.pid) {
			return next()
		}

		var rooms = ['category_' + topic.cid, 'recent_topics', 'popular_topics', 'unread_topics']
		var message = {status: post.status, tid: topic.tid}
		rooms.forEach(room => io.in(room).emit('kernel:topic', message))
		next()
	})
}

plugin.http = {};

plugin.http.get = (req, res, next) => {
	var data = {}

	var q = req.query.q
	var p = req.query.p

	if( (!string(q).isEmpty()) && (!string(p).isEmpty()) ) {
		return res.sendStatus(400)
	}

	if(!string(q).isEmpty()) {
		data.q = q;
		res.render('kernel', data)
	} else if(!string(p).isEmpty()){
		if(!string(p).isNumeric()) {
			return res.sendStatus(400)
		}
		async.waterfall([
			(next) => posts.exists(p, next),
			(exists, next) => {
				if(!exists) {
					return next('NOT_EXISTS')
				}
				posts.getPostField(p, 'content', next)
			},
			(content, next) => plugins.fireHook('filter:parse.raw', content, next),
			(html, next) => jsdom.env({html: html, src: [jquery], done: next}),
			(window, next) => {
				var codes = window.$("code[class='language-mma']")
				if(codes.length >= 1) {
					var scripts = []
					for(var i = 0; i < codes.length; i++) {
	          scripts.push(window.$(codes[i]).text())
	        }
					data.p = scripts
        }
				window.close();
				next()
			}
		], err => {
			if(err && err != 'NOT_EXISTS') {
				return res.sendStatus(500)
			}

			res.render('kernel', data)
		})
	} else {
		res.render('kernel');
	}

}

plugin.http.notebook = (req, res, next) => {
	var pid = req.query.pid
	if(!pid) {
		return res.sendStatus(400)
	}

	async.waterfall([
		(next) => posts.exists(pid, next),
		(exists, next) => {
			if(!exists) {
				return next('NOT_EXISTS')
			}

			posts.getPostField(pid, 'content', next)
		},
		(content, next) => plugins.fireHook('filter:parse.raw', content, next),
		(html, next) => jsdom.env({html: html, src: [jquery], done: next}),
		(window, next) => {
			var enunicode = (code) => {
			  code = code.replace(/[\u00FF-\uFFFF]/g,function($0){
			  		return '\\:'+$0.charCodeAt().toString(16);
			  });
			  return code
			};

			var cells = []
			var elements = window.$("body > p:not(:has(katex)),code[class='language-mma']")
			for(var i = 0; i < elements.length; i++) {
				var tagName = window.$(elements[i]).prop("tagName")
				var text = window.$(elements[i]).text()
				if(string(text).isEmpty()) continue;
				text = string(text).replaceAll('"', '\\"')
				text = enunicode(text)
				if(tagName == 'P') {
					cells.push({type: 'Text', value: text})
				} else if (tagName == 'CODE') {
					cells.push({type: 'Code', value: text})
				}
			}
			window.close();
			next(null, cells)
		},
		(cells, next) => {
			var content = "";
			cells.forEach((cell) => content += 'Cell["'+cell.value+'", "'+cell.type+'"],')
			var nb = fs.readFileSync('./node_modules/nodebb-plugin-kernel/static/notebook.nb', "utf-8")
			next(null, string(nb).replaceAll('(* Auto Generated Content *)', content))
		},
		(nb, next) => {
			var fileName = nconf.get('notebook:dir') + uuid.v1() + '.nb';
			fs.outputFileSync(fileName, nb)
			next(null, fileName)
		}
	], (err, fileName) => {
		if(err) {
			if(err == 'NOT_EXISTS') {
				return res.sendStatus(404)
			}
			next(err)
		}

		res.set('Content-Type', 'application/vnd.wolfram.mathematica;charset=UTF-8');
		return res.download(fileName)

	})


}

plugin.http.post = (req, res, next) => {
	var content = req.body.content
  var enable3d = req.body.enable3d
	if(!content) {
		return res.json({success: false, msg: '没有脚本可以运行', type: 'info'})
	}
	content = JSON.parse(content)
	var kernel = JSON.stringify({scripts:content})
  var path = (enable3d == 'true') ? '/evaluate-3d' : '/evaluate';
	var options = {
		path: path,
		method: 'POST',
		headers: {
			'Content-Type': 'application/json;charset=UTF-8',
			'Content-Length': Buffer.byteLength(kernel, 'utf8')
		},
		auth: nconf.get('kernel:username') + ':' + nconf.get('kernel:password')
	}

	async.waterfall([
		(next) => {
			var request = http.request(options, (response) => next(null, response))
			request.write(kernel)
			request.end()
		},
		(response, next) => {
			response.setEncoding('utf8')
      response.on('data', (chunk) => next(null, response.statusCode, chunk))
		},
		(statusCode, chunk, next) => {
			var json = {}
			if(statusCode == 200) {
				json = {success: true, result: chunk}
			} else if (statusCode == 502) {
				json = {success: false, msg: '计算服务目前不可用', type: 'danger'}
			} else if (statusCode == 500) {
				var act = JSON.parse(chunk);
				if(string(act.exception).contains('java.util.NoSuchElementException')) {
					json = {success: false, msg: '目前没有空余的计算内核,请稍后再试', type: 'info'}
				} else if(string(act.exception).contains('java.util.concurrent.TimeoutException')) {
					json = {success: false, msg: '这个计算太耗时了,算不出来啊', type: 'info'}
				} else {
					json = {success: false, msg: '计算服务错误,我们会尽快解决', type: 'danger'}
				}
			} else {
				json = {success: false, msg: '计算服务未知错误', type: 'danger'}
			}
			next(null, json)
		}
	], (err, json) => res.json(json)) //Ignore err because http does not produce that.
};

plugin.init = (data, next) => {
	next = next || function(){};

	helpers.setupPageRoute(data.router, '/kernel', data.middleware, [], plugin.http.get);
	data.router.post('/kernel', data.middleware.applyCSRF, plugin.http.post);
	data.router.get('/notebook', data.middleware.applyCSRF, plugin.http.notebook);
	modifyPages(next)
};

plugin.topic = {};

var populateStatus = (status, topicOrPost) => {
	if (status == 1) {
		topicOrPost.waiting = true;
	} else if (status == 2) {
		topicOrPost.evaluate = true;
	} else if (status == 3) {
		topicOrPost.finished = true;
	} else if (status == -1) {
		topicOrPost.error = true;
	} else if (status == -2) {
		topicOrPost.aborted = true;
	}
	return topicOrPost;
};

plugin.topic.list = (data, next) => {
	next = next || function(){};

	var list = data.topics
	async.map(list, (topic, next) => topics.getMainPost(topic.tid, topic.uid, (err, post) => {
		if(err) {
			return next(err)
		}

		topic = populateStatus(post.status, topic)
		next(null, topic)
	}), (err) => next(err, data))
};

plugin.topic.get = (data, next) => {
	next = next || function(){};

	async.map(data.topic.posts, (post, next) => {
		post = populateStatus(post.status, post)
		if(!post.result || post.result.length <= 0) {
			return next()
		}

		async.waterfall([
			(next) => jsdom.env({html: post.content, src: [jquery], done: next}),
			(window, next) => {
				var codes = window.$("code[class='language-mma']")
				for(var i = 0; i < post.result.length; i++) {
					if (post.result[i].type == 'text') {
						window.$(codes[post.result[i].index]).parent().append('<samp>'+post.result[i].data+'</samp>');
					} else if(post.result[i].type == 'error') {
						window.$(codes[post.result[i].index]).parent().append('<div class="kernel result alert alert-danger" role="alert">'+post.result[i].data+'</div>');
					} else if(post.result[i].type == 'abort') {
						window.$(codes[post.result[i].index]).parent().append('<div class="kernel result alert alert-warning" role="alert">计算超时</div>');
					} else if(post.result[i].type == 'image') {
						window.$(codes[post.result[i].index]).parent().append("<img class='kernel result img-responsive' src='/assets/kernel/post/"+post.pid+"/"+post.result[i].data+"'></img>");
					}
				}
				var html = window.document.documentElement.outerHTML;
				window.close();
				html = string(html).replaceAll('<html><head></head><body>', '').s;
				html = string(html).replaceAll('<script class="jsdom" src="http://www.wiseker.com/vendor/jquery/js/jquery.js"></script></body></html>', '').s;
				post.content = html;
				next()
			}
		], next)
	}, (err) => next(err, data))
};

plugin.post = {};

plugin.post.filterEdit = (postData, next) => {
	next = next || function(){};

	var conn = redis.createClient(nconf.get('redis:port'), nconf.get('redis:host'));
	conn.exists('post:lock:' + postData.post.pid, (err, re) => {
		conn.quit()
		if(err) {
			return next(err)
		}

		if(re == 1) {
			return next(new Error('帖子正在计算,暂时不能修改.'))
		}

		next(null, postData)
	})
};

plugin.post.filterPurge = (postData, next) => {
	next = next || function(){};

	var conn = redis.createClient(nconf.get('redis:port'), nconf.get('redis:host'));
	conn.exists('post:lock:' + postData.pid, (err, re) => {
		conn.quit()
		if(err) {
			return next(err)
		}

		if(re == 1) {
			return next(new Error('帖子正在计算,暂时不能清除.'))
		}

		next(null, postData)
	})
};

plugin.post.edit = (data, next) => {
	next = next || function(){};

	var post = data.post
	async.waterfall([
		(next) => db.deleteObjectField('post:' + post.pid, 'result', next),
		(next) => plugins.fireHook('filter:parse.raw', post.content, next),
		(html, next) => jsdom.env({html: html, src: [jquery], done: next}),
		(window, next) => {
			var codes = window.$("code[class='language-mma']");
			window.close();
			next(null, (codes && codes.length > 0) ? 1 : 0)
		},
		(status, next) => {
			post.status = status;
			posts.setPostField(post.pid, 'status', status, next)
		},
		(next) => emit(post, next),
		(next) => {
			var conn = new fivebeans.client(nconf.get('beanstalkd:host'), nconf.get('beanstalkd:port'));
			conn.on('connect', () => next(null, conn)).on('error', (err) => next(err)).connect();
		},
		(conn, next) => {
			conn.use('kernel', (err) => {
				if(err) {
					return next(err)
				}

				conn.put(Math.pow(2, 32), 0, 120, JSON.stringify({
					pid: post.pid,
					action: 'update'
				}), (err, jobid) => {
					conn.end();
					next(err, jobid);
				});
			})
		}
	], next)
};

plugin.post.save = (data, next) => {
	next = next || function(){};

	var post = data.post
	async.waterfall([
		(next) => plugins.fireHook('filter:parse.raw', post.content, next),
		(html, next) => jsdom.env({html: html, src: [jquery],done: next}),
		(window, next) => {
			var codes = window.$("code[class='language-mma']");
			window.close();
			next(null, (codes && codes.length > 0) ? 1 : 0)
		},
		(status, next) => {
			post.status = status;
			posts.setPostField(post.pid, 'status', status, (err) => {
				if(err) {
					return next(err)
				}
				if(status == 0) {
					return next('NO_NEED_TO_SEND')
				}

				next()
			});
		},
		(next) => emit(post, next),
		(next) => {
			var conn = new fivebeans.client(nconf.get('beanstalkd:host'), nconf.get('beanstalkd:port'));
			conn.on('connect', () => next(null, conn)).on('error', (err) => next(err)).connect();
		},
		(conn, next) => {
			conn.use('kernel', (err) => {
				if (err) {
					return next(err)
				}

				conn.put(Math.pow(2, 32), 0, 120, JSON.stringify({
					pid: post.pid,
					action: 'create'
				}), (err, jobid) => {
					conn.end();
					next(err, jobid);
				});
			});
		}
	], (err) => {
		if(err && err != 'NO_NEED_TO_SEND') {
			return next(err)
		}

		next()
	})
};

plugin.post.purge = (data, next) => {
	next = next || function(){};

	var pid = data.post.pid
	async.waterfall([
		(next) => {
			var conn = new fivebeans.client(nconf.get('beanstalkd:host'), nconf.get('beanstalkd:port'));
			conn.on('connect', () => next(null, conn)).on('error', (err) => next(err)).connect();
		},
		(conn, next) => {
			conn.use('kernel', (err) => {
				if (err) {
					return next(err)
				}

				conn.put(Math.pow(2, 32), 0, 120, JSON.stringify({
					pid: pid,
					action: 'purge'
				}), (err, jobid) => {
					conn.end();
					next(err, jobid)
				});
			});
		}
	], next)
};

plugin.registerFormatting = (payload, next) => {
	var formatting = ['code'];

	formatting.reverse();
	formatting.forEach((format) => {
		payload.options.unshift({
			name: format,
			className: 'fa fa-' + format
		});
	});

	next(null, payload);
}

module.exports = plugin;
