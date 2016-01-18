'use strict';

var MarkdownIt = require('markdown-it'),
md = new MarkdownIt({
  'langPrefix': 'language-'
}),
fs = require('fs-extra'),
jquery = fs.readFileSync("./jquery-2.1.4.min.js", "utf-8"),
jsdom = require("jsdom"),
async = require('async'),
http = require('http'),
string = require('string'),
notification = require("./notification");

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
        if(post.pid == topic.mainPid) {
          var message = {status: post.status, tid: topic.tid};
          io.to('category_' + topic.cid).emit('kernel:topic', message);
          io.to('recent_topics').emit('kernel:topic', message);
          io.to('popular_topics').emit('kernel:topic', message);
          io.to('unread_topics').emit('kernel:topic', message);
        }
        return callback(null, null);
      }
    ], callback);
  };

  var kernel = function (post, pid, dir, url, username, password, callback) {
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
              },
              auth: username + ':' + password
            };
            var request = http.request(options, function(response) {
              response.setEncoding('utf8');
              response.on('data', function(chunk) {
                var set = {};
                var needRelease = false;
                if(response.statusCode == 200 || response.statusCode == 500) {
                  var act = JSON.parse(chunk);
                  if(response.statusCode == 200) {
                    set.status = 3;
                    var lastData = act[act.length - 1];
                    if(lastData.type == 'error') {
                      set.status = -1;
                    } else if (lastData.type == 'abort') {
                      set.status = -2;
                    }
                    set.result = act;
                  } else { // 500
                    if(string(act.exception).contains('java.util.concurrent.TimeoutException')) {
                      set.status = -2;
                    } else {
                      set.status = 1;
                      needRelease = true;
                    }
                  }
                } else {
                  set.status = 1;
                  needRelease = true;
                }
                db.collection("objects").updateOne({_key:'post:' + pid}, {$set:set}, function (err) {
                  return callback(err, needRelease);
                });
              });// end of response.on('data'...
            });// end of http.request
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
          if(needRelease) {
            return callback(null, true);
          } else {
            notify.notice(post, function(err) {
              return callback(err, false);
            })
          }
        })//end of emit
      }
    }); // end of waterfall
  };

  var db = db;

  var io = require('socket.io-emitter')(redis);

  var notify = new notification(db, io);

  this.update = function (jobData, callback) {
    var dir = jobData.dir,
    pid = jobData.pid,
    url = jobData.url,
    username = jobData.username,
    password = jobData.password;
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
        return kernel(post, pid, dir, url, username, password, callback);
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
    url = jobData.url,
    username = jobData.username,
    password = jobData.password;
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
      return kernel(post, pid, dir, url, username, password, callback);
    });
  };

  this.clean = function(dir, second, callback) {
    var total = 0;
    var current = new Date();
    fs.walk(dir).on('data', function(file) {
      if(file.stats.isFile()) {
        var createTime = file.stats.birthtime.getTime();
        var span = ((current.getTime() - createTime) / 1000);
        if(span > second) {
          fs.removeSync(file.path);
          total++;
        }
      }
    })
    .on('end', function () {
      callback(null, total);
    })
  };

}

module.exports = BeanstalkdService;
