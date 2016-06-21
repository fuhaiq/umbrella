'use strict';

var log4js = require('log4js');
log4js.configure({
	appenders: [{
		type: "console"
	}],
	replaceConsole: true
});
global.LOG = log4js.getLogger();

var fs = require('fs-extra'),
jquery = fs.readFileSync("./jquery-2.1.4.min.js", "utf-8"),
config = fs.readJsonSync("./config.json"),
jsdom = require("jsdom"),
MongoClient = require('mongodb').MongoClient,
redis = require('redis'),
async = require('async'),
BeanstalkdService = require("./BeanstalkdService"),
fivebeans = require('fivebeans'),
string = require('string'),
CronJob = require("cron").CronJob,
db = null,
redisClient = null,
beanstalkd = null,
service = null;

var reserve = () => {
	async.waterfall([
		(next) => beanstalkd.reserve(next),
		(jobid, payload, next) => {
			var job = JSON.parse(payload.toString());
			job.id = jobid;
			job.dir = config.kernel.imgDir;
			job.url = config.kernel.url;
			job.username = config.kernel.username;
			job.password = config.kernel.password;
			if(!string(job.pid).isNumeric()) {
				return next('回复pid为不是数字');
			}
			if(string(job.dir).isEmpty()) {
				return next('回复文件路径dir为空');
			}
			if(string(job.url).isEmpty()) {
				return next('计算url为空', null);
			}
			if(string(job.username).isEmpty()) {
				return next('kernel认证用户名为空');
			}
			if(string(job.password).isEmpty()) {
				return next('kernel认证密码为空');
			}
			next(null, job)
		},
		(job, next) => redisClient.setnx('post:lock:' + job.pid, 'locked', (err, buffer) => next(err, job, (buffer.toString() == 0))),
		(job, locked, next) => {
			if(locked) {
				LOG.warn('回复[post:'+job.pid+']被锁定,延时5秒后重新调度任务[jobid:'+job.id+']');
				beanstalkd.release(job.id, Math.pow(2, 32), 5, (err) => next(err || 'POST_LOCKED'))
			} else {
				next(null, job)
			}
		},
		(job, next) => {
			var fn = null;
			if(job.action == 'create') {
				fn = service.create;
			} else if (job.action == 'update') {
				fn = service.update;
			}	else if (job.action == 'purge') {
				fn = service.purge;
			}	else {
				LOG.warn('没有此类型['+job.action+']任务,直接删除');
				fn = (job, next) => next(null, false)
			}
			fn(job, (err, needRelease) => next(err, job, needRelease))
		},
		(job, needRelease, next) => {
			if(needRelease) {
				LOG.warn('回复[post:'+job.pid+']未操作,延时5秒后重新调度任务[jobid:'+job.id+']')
				beanstalkd.release(job.id, Math.pow(2, 32), 5, err => next(err, job))
			} else {
				beanstalkd.destroy(job.id, err => next(err, job))
			}
		},
		(job, next) => redisClient.del('post:lock:' + job.pid, next)
	], (err) => {
		if(err && err != 'POST_LOCKED') {
			LOG.error(err);
		}
		reserve()
	})
};

async.parallel({
	mongodb: (next) => MongoClient.connect(config.mongo.url, next),
	redis: (next) => {
		var conn = redis.createClient(config.redis.port, config.redis.host, {
			return_buffers : true
		})
		conn.on('connect', () => next(null, conn))
		conn.on('error', err => next(err))
	},
	beanstalkd: (next) => {
		var conn = new fivebeans.client(config.beanstalkd.host, config.beanstalkd.port)
		conn.on('connect', () => next(null, conn)).on('error', err => next(err)).connect()
	}
}, (err, conns) => {
	if(err) {
		LOG.error(err)
		return;
	}

	db = conns.mongodb;
	redisClient = conns.redis;
	beanstalkd = conns.beanstalkd;
	service = new BeanstalkdService(db, redisClient);

	beanstalkd.watch(config.beanstalkd.tube, err => {
	  if(err) {
	    LOG.error(err)
	  } else {
			LOG.info('#############开始监听任务#############')
	    reserve()
	  }
	})

	new CronJob({
	  cronTime: config.clean.cronTime,
	  onTick: () => {
	    service.clean(config.clean.dir, config.clean.second, (err, total) =>{
	      if(err) {
	        LOG.error(err)
	      }else if (total > 0) {
	        LOG.warn('共删除个'+total+'文件');
	      }
	    })
	  },
	  start: true,
	  timeZone: config.clean.timeZone
	}).start()

})

/* shutdown event for pm2 */
process.on('SIGTERM', function() {
	if(db) {
		db.close();
	}
	if(redisClient) {
		redisClient.quit();
	}
	if(beanstalkd) {
		beanstalkd.quit();
	}
	process.exit(0)
});

/* shutdown event for Ctrl+C */
process.on('SIGINT', function() {
	if(db) {
		db.close();
	}
	if(redisClient) {
		redisClient.quit();
	}
	if(beanstalkd) {
		beanstalkd.quit();
	}
	process.exit(0)
});
