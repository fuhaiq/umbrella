"use strict";

$('document').ready(function() {

	$(window).on('action:composer.loaded', function(err, data) {
		if (data.hasOwnProperty('composerData') && !data.composerData.isMain) {
			// Do nothing, as this is a reply, not a new post
			return;
		}

		var item = $('<li><a href="#" data-switch-action="evaluate"><i class="fa fa-fw fa-play"></i> 执行脚本</a></li>');
		$('#cmp-uuid-' + data.post_uuid + ' .action-bar .dropdown-menu').append(item);

		item.on('click', function() {
			// $(window).one('action:composer.topics.post', function(ev, data) {
			// 	callToggleQuestion(data.data.tid);
			// });
			console.log('evaluate');
		});

	});

});