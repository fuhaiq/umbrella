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

var reserve = function () {
    async.waterfall([
        function (callback) {
            LOG.info('开始接收beanstalkd任务');
            beanstalkd.reserve(callback);
        },
        function (jobid, payload, callback) {
            var jobData = JSON.parse(payload.toString());
            jobData.dir = config.kernel.imgDir;
            jobData.url = config.kernel.url;
						jobData.username = config.kernel.username;
						jobData.password = config.kernel.password;
            if(!string(jobData.pid).isNumeric()) {
                return callback('回复pid为不是数字', null);
            }
            if(string(jobData.dir).isEmpty()) {
                return callback('回复文件路径dir为空', null);
            }
            if(string(jobData.url).isEmpty()) {
                return callback('计算url为空', null);
            }
						if(string(jobData.username).isEmpty()) {
                return callback('kernel认证用户名为空', null);
            }
						if(string(jobData.password).isEmpty()) {
                return callback('kernel认证密码为空', null);
            }
            return callback(null, jobid, jobData);
        },
        function (jobid, jobData, callback) {
            redisClient.setnx('post:lock:' + jobData.pid, 'locked' , function (err, buffer) {
                if(err) {
                    return callback(err);
                }
                var locked = buffer.toString();
                if(locked == 0) {
                    LOG.warn('回复[post:'+jobData.pid+']被锁定,延时5秒后重新调度任务[jobid:'+jobid+']');
                    beanstalkd.release(jobid, Math.pow(2, 32), 5, callback);
                } else {
                    async.waterfall([
                        function (callback) {
                            var fn = null;
                            switch(jobData.action) {
                                case 'create' :
                                    fn = service.create;
                                    break;
                                case 'update' :
                                    fn = service.update;
                                    break;
                                case 'purge' :
                                    fn = service.purge;
                                    break;
                                default :
                                    LOG.warn('没有此类型['+jobData.action+']任务,直接删除');
                                    fn = function (jobData, callback) {
                                        return callback(null, false);
                                    };
                            }
                            return fn(jobData, callback);
                        },
                        function (needRelease, callback) {
                            if(needRelease) {
                                LOG.warn('回复[post:'+jobData.pid+']未操作,延时5秒后重新调度任务[jobid:'+jobid+']')
                                beanstalkd.release(jobid, Math.pow(2, 32), 5, callback);
                            } else {
                                LOG.info('完成操作,删除任务')
                                beanstalkd.destroy(jobid, callback);
                            }
                        }
                    ], function (err) {
                        redisClient.del('post:lock:' + jobData.pid, function () {
                            if(err) {
                                LOG.error('操作出错,bury任务[jobid:'+jobid+']')
                                beanstalkd.bury(jobid, Math.pow(2, 32), function () {
                                    return callback(err);
                                });
                            } else {
                                return callback(null, null);
                            }
                        });
                    });//end of waterfall
                }//end of else, means the post is locked.
            });//end of redis.setnx, try lock.
        }//end of function
    ], function (err) {
        if(err) {
            LOG.error(err);
        }
        reserve();
    });
};

async.parallel({
    mongodb: function (callback) {
        MongoClient.connect(config.mongo.url, function (err, db) {
            if(err) {
                LOG.error('链接mongodb出错:');
                return callback(err);
            }
            return callback(null, db);
        });
    },
    redis: function (callback) {
        var client = redis.createClient(config.redis.port, config.redis.host, {
            return_buffers : true
        });
        client.on("connect", function (){
            return callback(null, client);
        });
        client.on("error", function (err) {
            LOG.error('链接redis出错:');
            return callback(err);
        });
    },
    beanstalkd: function (callback) {
        var client = new fivebeans.client(config.beanstalkd.host, config.beanstalkd.port);
        client.on('connect', function () {
            return callback(null, client);
        })
        .on('error', function (err) {
            LOG.error('链接beanstalkd出错:');
            return callback(err);
        })
        .connect();
    }
},function (err, results){
    db = results.mongodb;
    redisClient = results.redis;
    beanstalkd = results.beanstalkd;
    if(err) {
        LOG.error(err);
    } else {
        service = new BeanstalkdService(db, redisClient);
        beanstalkd.watch(config.beanstalkd.tube, function (err) {
            if(err) {
                LOG.error(err);
            } else {
                reserve();
            }
        });
				//start clean cron job
				var cleanJob = new CronJob({
				  cronTime: config.clean.cronTime,
				  onTick: function() {
						LOG.info('执行清理任务');
						service.clean(config.clean.dir, config.clean.second, function(err, total){
							if(err) {
								LOG.error(err);
							} else {
								if(total > 0) {
					        LOG.warn('共删除个'+total+'文件');
					      }
								LOG.info('执行清理任务完毕');
							}
						});
				  },
				  start: true,
				  timeZone: config.clean.timeZone
				});
				cleanJob.start();
    }
});

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
