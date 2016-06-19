"use strict";

var plugin = {},
async = module.parent.require('async'),
topics = module.parent.require('./topics'),
posts = module.parent.require('./posts'),
winston = module.parent.require('winston'),
fs = require('fs-extra'),
db = module.parent.require('./database'),
categories = module.parent.require('./categories'),
nconf = module.parent.require('nconf');

var modifyPages = (next) => {
	next = next || function() {};

	async.series([
		(next) => {
			var originalCategoriesPage = fs.readFileSync("./public/templates/categories.tpl", "utf-8")
			var modify = fs.readFileSync("./node_modules/nodebb-plugin-template/templates/categories/card.tpl", "utf-8")
			fs.outputFile('./public/templates/categories.tpl', modify + originalCategoriesPage, next)
		},
		(next) => {
			var originalCategoriesPage = fs.readFileSync("./public/templates/categories.tpl", "utf-8")
			var modify = fs.readFileSync("./node_modules/nodebb-plugin-template/templates/categories/categories.tpl", "utf-8")
			modify = modify.replace('<!-- Categories Original content -->', originalCategoriesPage)
			fs.outputFile('./public/templates/categories.tpl', modify, next)
		}
	], next)
}

plugin.init = (data, next) => {
	next = next || function(){};

	modifyPages(next)
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

var getStatusTopics = (status, num, next) => {
	db.client.collection('objects').find({_key:/^post:\d+$/, status:status},{_id:0,pid:1}).sort({timestamp:-1}).limit(num).toArray((err, docs) => {
		if(err) {
			return next(err)
		}

		posts.getPostSummaryByPids(docs.map(doc => doc.pid), 0, {stripTags: true}, next)
	})
}

var getCard = (data, next) => {
	var defaultSettings = { opacity: '1.0', textShadow: 'none' };

	topics.getTopicsFromSet('topics:recent', data.req.uid, 0, 19, function(err, topics) {
		if (err) {
			return next(err);
		}

		var i = 0, cids = [], finalTopics = [];
		while (finalTopics.length < 4 && i < topics.topics.length) {
			var cid = parseInt(topics.topics[i].cid, 10);

			if (cids.indexOf(cid) === -1) {
				cids.push(cid);
				finalTopics.push(topics.topics[i]);
			}

			i++;
		}

		async.each(finalTopics, function (topic, next) {
			categories.getCategoryField(topic.cid, 'image', function (err, image) {
				topic.category.backgroundImage = image;
				next();
			});
		}, function () {
			data.templateData.topics = finalTopics;
			data.templateData.recentCards = {
				opacity: defaultSettings.opacity,
				textShadow: defaultSettings.textShadow
			};
			next();
		});
	});
}

plugin.getCategories = (data, next) => {
	var templateData = data.templateData;

	async.parallel({
		tags : (next) => getPopularTags(next),
		success: (next) => getStatusTopics(3, 6, next),
		syntax: (next) => getStatusTopics(-1, 6, next),
		aborted: (next) => getStatusTopics(-2, 6, next),
		card: (next) => getCard(data, next)
	}, (err, result) => {
		if(err) {
			return next(err)
		}
		data.templateData.tags = result.tags
		data.templateData.success = result.success
		data.templateData.syntax = result.syntax
		data.templateData.aborted = result.aborted
		next(null, data)
	})
}

module.exports = plugin;
