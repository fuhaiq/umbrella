"use strict";

var plugin = {},
async = module.parent.require('async'),
emitter = module.parent.require('./emitter'),
topics = module.parent.require('./topics'),
winston = module.parent.require('winston'),
fs = require('fs'),
path = require('path'),
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

plugin.getCategories = (data, next) => {
	var templateData = data.templateData;

	topics.getTags(0, 20, (err, tags) => {
		if (err) {
			return next(err);
		}

		templateData.tags = tags.filter(tag => {
			return tag && tag.score > 0;
		})
		next(null, data);
	})
}


module.exports = plugin;
