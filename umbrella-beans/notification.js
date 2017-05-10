'use strict';

var async = require('async'),
  uuid = require('uuid'),
  batch = require("./batch"),
  string = require('string');

var notification = function(db, io) {

  var db = db;

  var io = io;

  var valueToString = function(value) {
  	if(value === null || value === undefined) {
  		return value;
  	}
  	return value.toString();
  };

  var getObject = function(key, callback) {
		if (!key) {
			return callback();
		}
		db.collection('objects').findOne({_key: key}, {_id: 0, _key: 0}, callback);
	};

  var setObject = function(key, data, callback) {
		callback = callback || function() {};
		if (!key) {
			return callback();
		}
		db.collection('objects').update({_key: key}, {$set: data}, {upsert: true, w: 1}, function(err) {
			callback(err);
		});
	};

  var sortedSetAddBulk = function(key, scores, values, callback) {
		if (!scores.length || !values.length) {
			return callback();
		}
		if (scores.length !== values.length) {
			return callback('数据错误,长度不对等');
		}
		values = values.map(valueToString);
		var bulk = db.collection('objects').initializeUnorderedBulkOp();
		for(var i=0; i<scores.length; ++i) {
			bulk.find({_key: key, value: values[i]}).upsert().updateOne({$set: {score: parseInt(scores[i], 10)}});
		}
		bulk.execute(function(err, result) {
			callback(err);
		});
	};

  var sortedSetAdd = function(key, score, value, callback) {
		callback = callback || function() {};
		if (!key) {
			return callback();
		}
		if (Array.isArray(score) && Array.isArray(value)) {
			return sortedSetAddBulk(key, score, value, callback);
		}
		value = valueToString(value);
		db.collection('objects').update({_key: key, value: value}, {$set: {score: parseInt(score, 10)}}, {upsert:true, w: 1}, function(err) {
			callback(err);
		});
	};

  var sortedSetsAdd = function(keys, score, value, callback) {
		callback = callback || function() {};
		if (!Array.isArray(keys) || !keys.length) {
			return callback();
		}
		value = valueToString(value);
		var bulk = db.collection('objects').initializeUnorderedBulkOp();
		for(var i=0; i<keys.length; ++i) {
			bulk.find({_key: keys[i], value: value}).upsert().updateOne({$set: {score: parseInt(score, 10)}});
		}
		bulk.execute(function(err, result) {
			callback(err);
		});
	};

  var sortedSetsRemove = function(keys, value, callback) {
		callback = callback || function() {};
		if (!Array.isArray(keys) || !keys.length) {
			return callback();
		}
		value = valueToString(value);
		db.collection('objects').remove({_key: {$in: keys}, value: value}, function(err, res) {
			callback(err);
		});
	};

  var sortedSetsRemoveRangeByScore = function(keys, min, max, callback) {
		callback = callback || function() {};
		if (!Array.isArray(keys) || !keys.length) {
			return callback();
		}

    var query = {_key: {$in: keys}};

		if (min !== '-inf') {
			query.score = {$gte: min};
		}
		if (max !== '+inf') {
			query.score = query.score || {};
			query.score.$lte = max;
		}

		db.collection('objects').remove(query, function(err) {
			callback(err);
		});
	};

  var pushToUids = function(uids, notification, callback) {
		var oneWeekAgo = Date.now() - 604800000;
		var unreadKeys = [];
		var readKeys = [];

		async.waterfall([
			function (next) {
        next(null, {notification: notification, uids: uids})
			},
			function (data, next) {
				uids = data.uids;
				notification = data.notification;

				uids.forEach(function(uid) {
					unreadKeys.push('uid:' + uid + ':notifications:unread');
					readKeys.push('uid:' + uid + ':notifications:read');
				});

				sortedSetsAdd(unreadKeys, notification.datetime, notification.nid, next);
			},
			function (next) {
				sortedSetsRemove(readKeys, notification.nid, next);
			},
			function (next) {
				sortedSetsRemoveRangeByScore(unreadKeys, '-inf', oneWeekAgo, next);
			},
			function (next) {
				sortedSetsRemoveRangeByScore(readKeys, '-inf', oneWeekAgo, next);
			},
			function (next) {
        uids.forEach(function(uid) {
          io.in('uid_' + uid).emit('event:new_notification', notification);
        });
				next();
			}
		], callback);
	}

  var create = function(data, callback) {
		if (!data.nid) {
			return callback('no-notification-id: notification 没有 ID');
		}
		data.importance = data.importance || 5;
		getObject('notifications:' + data.nid, function(err, oldNotification) {
			if (err) {
				return callback(err);
			}

			if (oldNotification) {
				if (parseInt(oldNotification.pid, 10) === parseInt(data.pid, 10) && parseInt(oldNotification.importance, 10) > parseInt(data.importance, 10)) {
					return callback();
				}
			}

			var now = Date.now();
			data.datetime = now;
			async.parallel([
				function(next) {
					sortedSetAdd('notifications', now, data.nid, next);
				},
				function(next) {
					setObject('notifications:' + data.nid, data, next);
				}
			], function(err) {
				callback(err, data);
			});
		});
	};

  var push = function(notification, uids, callback) {
		callback = callback || function() {};

		if (!notification.nid) {
			return callback();
		}

		if (!Array.isArray(uids)) {
			uids = [uids];
		}

    uids = uids.filter(function(uid, index, array) {
			return parseInt(uid, 10) && array.indexOf(uid) === index;
		});

		if (!uids.length) {
			return callback();
		}

    setTimeout(function () {
			batch.processArray(uids, function (uids, next) {
				pushToUids(uids, notification, next);
			}, { interval: 1000 }, function (err) {
				if (err) {
					LOG.error(err.stack);
				}
			});
		}, 1000);

		callback();
	};

  this.notice = function(post, callback) {
    async.waterfall([
      function(callback){
        db.collection("objects").findOne({_key:'topic:' + post.tid}, callback);
      },
      function(topic, callback) {
        var msg = (post.pid == topic.mainPid) ? topic.title : 'RE: ' + topic.title;
        var notification = {
          bodyShort: '[[kernel:notification.post_done, ' + msg + ']]',
          nid: 'kernel:post:' + post.pid + ':uid:' + post.uid + ":id:" + uuid.v1(),
          path: '/post/' + post.pid,
          pid : post.pid,
          datetime: Date.now()
        };
        return callback(null, notification);
      },
      function(notification, callback) {
        create(notification, callback);
      },
      function(notification, callback) {
        push(notification, post.uid, callback);
      }
    ], callback);
  };

}

module.exports = notification;
