"use strict";

var plugin = {},
async = module.parent.require('async'),
emitter = module.parent.require('./emitter'),
topics = module.parent.require('./topics'),
posts = module.parent.require('./posts'),
winston = module.parent.require('winston'),
fs = require('fs'),
path = require('path'),
db = module.parent.require('./database'),
nconf = module.parent.require('nconf');

var modifyMainPage = (next) => {
	next = next || function() {};

	var tplPath = path.join(nconf.get('base_dir'), 'public/templates/categories.tpl'),
	cardPath = path.join(nconf.get('base_dir'), 'node_modules/nodebb-plugin-recent-cards/static/templates/partials/nodebb-plugin-recent-cards/header.tpl'),
	mainPath = path.join(nconf.get('base_dir'), 'node_modules/nodebb-plugin-template/templates/main/main.tpl');

	async.parallel({
		original: (next) => fs.readFile(tplPath, next),
		card: (next) => fs.readFile(cardPath, next),
		main: (next) => fs.readFile(mainPath, next)
	}, (err, tpls) => {
		if(err) {
			return next(err)
		}

		var card = tpls.card.toString();
		var original = tpls.original.toString();
		var main = tpls.main.toString();
		if (!original.match('<!-- Main plugin -->')) {
			main = main.replace('<!-- Main Original content -->', card + original)
		}

		fs.writeFile(tplPath, main, next);
	});

}

plugin.init = (data, next) => {
	next = next || function(){};

	modifyMainPage(next)
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

plugin.getCategories = (data, next) => {
	var templateData = data.templateData;

	async.parallel({
		tags : (next) => getPopularTags(next),
		success: (next) => getStatusTopics(3, 6, next),
		syntax: (next) => getStatusTopics(-1, 6, next),
		aborted: (next) => getStatusTopics(-2, 6, next)
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
