"use strict";

var plugin = {},
async = module.parent.require('async'),
topics = module.parent.require('./topics'),
posts = module.parent.require('./posts'),
winston = module.parent.require('winston'),
fs = require('fs-extra'),
db = module.parent.require('./database'),
nconf = module.parent.require('nconf');

plugin.init = (data, next) => {
	next = next || function(){};

	next()
};

var getPopularTags = (next) => {
	topics.getTags(0, 20, (err, tags) => {
		if (err) {
			return next(err);
		}

		tags = tags.filter(tag => {
			return tag && tag.score > 0;
		})
		next(null, tags);
	})
}

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

var getKernelPosts = (num, next) => {
	db.client.collection('objects').find({_key:/^post:\d+$/, status:{$in:[3,-1,-2]}},{_id:0,pid:1}).sort({timestamp:-1}).limit(num).toArray((err, docs) => {
		if(err) {
			return next(err)
		}

		posts.getPostSummaryByPids(docs.map(doc => doc.pid), 0, {stripTags: true}, (err, docs) => {
			if(err) {
				return next(err)
			}

			docs.forEach(doc => {
				doc = populateStatus(doc.status, doc)
			})
			next(null, docs)
		})
	})
}

plugin.getPostSummaryByPids = (postData, next) => {
	async.eachSeries(postData.posts, (post, next) => {
		posts.getPostField(post.pid, 'status', (err, status) => {
			if (err) {
				return next(err);
			}

			post.status = status;
			next()
		})
	}, err => next(err, postData))
}

plugin.getRecent = (data, next) => {
	var templateData = data.templateData;

	async.parallel({
		tags : (next) => getPopularTags(next),
		kernelPosts: (next) => getKernelPosts(5, next)
	}, (err, result) => {
		if(err) {
			return next(err)
		}

		data.templateData.tags = result.tags
		data.templateData.kernelPosts = result.kernelPosts
		next(null, data)
	})
}

module.exports = plugin;
