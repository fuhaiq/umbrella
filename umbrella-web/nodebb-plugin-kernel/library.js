"use strict";

var net = require('net');

var plugin = {};

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
		console.error(err);
		return res.sendStatus(500);
	});
}

plugin.init = function(data, callback) {

	data.router.get('/api/kernel', plugin.get);

	data.router.post('/kernel', data.middleware.applyCSRF, plugin.post);

	callback();
};



module.exports = plugin;