'use strict';

var MarkdownIt = require('markdown-it'),
md = new MarkdownIt({
  'langPrefix': 'language-'
}),
fs = require('fs-extra'),
klaw = require('klaw'),
jquery = fs.readFileSync("./jquery-2.1.4.min.js", "utf-8"),
jsdom = require("jsdom/lib/old-api.js"),
async = require('async'),
http = require('http'),
string = require('string'),
notification = require("./notification");

var BeanstalkdService = function(db, redis) {

  var emit = (post, next) => {
    async.waterfall([
      (next) => db.collection("objects").findOne({_key:'topic:' + post.tid}, next),
      (topic, next) => {
        if(post.pid != topic.mainPid) {
          return next('NO_NEED_TO_EMIT')
        }

        next(null, ['category_' + topic.cid, 'recent_topics', 'popular_topics', 'unread_topics'], {status: post.status, tid: topic.tid})
      },
      (rooms, message, next) => {
        rooms.forEach(room => io.to(room).emit('kernel:topic', message))
        next()
      }
    ], (err) => {
      if(err && err != 'NO_NEED_TO_EMIT') {
        return next(err)
      }
      next()
    })
  };

  var kernel = (post, dir, url, username, password, next) => {
    async.waterfall([
      (next) => next(null, md.render(post.content)),
      (html, next) => jsdom.env({html: html, src: [jquery], done: next}),
      (window, next) => {
        var codes = window.$("code[class='language-mma']");
        if(codes.length < 1) {
          window.close();
          return next('回复[post:'+post.pid+']内容找不到执行脚本');
        }

        var scripts = []
        for(var i = 0; i < codes.length; i++) {
          scripts.push(window.$(codes[i]).text())
        }
        window.close();
        next(null, scripts)
      },
      (scripts, next) => {
        post.status = 2
        emit(post, err => next(err, scripts))
      },
      (scripts, next) => db.collection("objects").updateOne({_key:'post:' + post.pid}, {$set: {status: post.status}}, err => next(err, scripts)),
      (scripts, next) => {
        fs.mkdirsSync(dir + post.pid);
        var kernel = JSON.stringify({dir: dir + post.pid + '/', scripts:scripts})
        var options = {
          hostname: url.hostname,
          path:url.path,
          port:url.port,
          method: 'POST',
          headers: {
            'Content-Type': 'application/json;charset=UTF-8',
            'Content-Length': Buffer.byteLength(kernel, 'utf8')
          },
          auth: username + ':' + password
        };
        var request = http.request(options, response => next(null, response))
        request.write(kernel);
        request.end();
      },
      (response, next) => {
        response.setEncoding('utf8');
        response.on('data', chunk => next(null, response.statusCode, chunk))
      },
      (statusCode, chunk, next) => {
        var pack = {set : {}, needRelease : false};
        if(statusCode == 200) {
          var act = JSON.parse(chunk);
          pack.set.status = 3;
    		  if(act.length > 0) {
    			  var lastData = act[act.length - 1];
    			  if(lastData.type == 'error') {
    				pack.set.status = -1;
    			  } else if (lastData.type == 'abort') {
    				pack.set.status = -2;
    			  }
    			  pack.set.result = act;
    		  }
        } else if (statusCode == 500) {
          var act = JSON.parse(chunk);
          if(string(act.exception).contains('java.util.concurrent.TimeoutException')) {
            pack.set.status = -2;
          } else {
            pack.set.status = 1;
            pack.needRelease = true;
          }
        } else {
          pack.set.status = 1;
          pack.needRelease = true;
        }
        next(null, pack);
      },
      (pack, next) => {
        post.status = pack.set.status;
        db.collection("objects").updateOne({_key:'post:' + post.pid}, {$set:pack.set}, err => next(err, pack.needRelease))
      },
      (needRelease, next) => emit(post, err => next(err, needRelease)),
      (needRelease, next) => {
        if(needRelease) {
          return next(null, needRelease)
        }

        notify.notice(post, err => next(err, needRelease))
      }
    ], (err, needRelease) => {
      if(err) {
        db.collection("objects").updateOne({_key:'post:' + post.pid}, {$set: {status: 1}}, dbErr => next(err || dbErr))
      } else {
        next(null, needRelease)
      }
    })
  };

  var db = db;

  var io = require('socket.io-emitter')(redis, {
    key : 'db:0:adapter_key'
  });

  var notify = new notification(db, io);

  this.update = (job, next) => {
    async.waterfall([
      (next) => db.collection("objects").findOne({_key:'post:' + job.pid}, next),
      (post, next) => {
        if(!post) {
          return next('回复[post:'+job.pid+']不存在')
        }

        if(post.status == 0) {
          fs.removeSync(job.dir + job.pid)
          emit(post, err => next(err, false))
        } else if (post.status == 3 || post.status == 2 || post.status == -1 || post.status == -2) {
          LOG.warn('回复[post:'+post.pid+'] 已经被计算,或者正在被计算.')
          next(null, false)
        } else if (post.status == 1) {
          fs.removeSync(job.dir + job.pid)
          kernel(post, job.dir, job.url, job.username, job.password, next)
        } else {
          next('回复[post:'+post.pid+']状态错误['+post.status+']')
        }
      }
    ], next)
  };

  this.purge = (job, next) => {
    fs.removeSync(job.dir + job.pid)
    next(null, false)
  };

  this.create = (job, next) => {
    async.waterfall([
      (next) => db.collection("objects").findOne({_key:'post:' + job.pid}, next),
      (post, next) => {
        if(!post) {
          return next('回复[post:'+job.pid+']不存在')
        }

        if(post.status != 1) {
          return next('回复[post:'+post.pid+']状态错误['+post.status+'], 期望值: 1')
        }

        kernel(post, job.dir, job.url, job.username, job.password, next)
      }
    ], next)
  };

  this.clean = (dir, second, next) => {
    var total = 0;
    var current = new Date();
    klaw(dir).on('data', (file) => {
      if(file.stats.isFile()) {
        var createTime = file.stats.birthtime.getTime();
        var span = ((current.getTime() - createTime) / 1000);
        if(span > second) {
          fs.removeSync(file.path);
          total++;
        }
      }
    }).on('end', () => next(null, total))
  };

}

module.exports = BeanstalkdService;
