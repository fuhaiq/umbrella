'use strict';

var async = require('async');

var DEFAULT_BATCH_SIZE = 100;

exports.processArray = function (array, process, options, callback) {
	if (typeof options === 'function') {
		callback = options;
		options = {};
	}

	callback = typeof callback === 'function' ? callback : function () {};
	options = options || {};

	if (!Array.isArray(array) || !array.length) {
		return callback();
	}
	if (typeof process !== 'function') {
		return callback(new Error('[[error:process-not-a-function]]'));
	}

	var batch = options.batch || DEFAULT_BATCH_SIZE;
	var start = 0;
	var done = false;

	async.whilst(
		function () {
			return !done;
		},
		function (next) {
			var currentBatch = array.slice(start, start + batch);
			if (!currentBatch.length) {
				done = true;
				return next();
			}
			process(currentBatch, function (err) {
				if (err) {
					return next(err);
				}
				start += batch;
				if (options.interval) {
					setTimeout(next, options.interval);
				} else {
					next();
				}
			});
		},
		function (err) {
			callback(err);
		}
	);
};
