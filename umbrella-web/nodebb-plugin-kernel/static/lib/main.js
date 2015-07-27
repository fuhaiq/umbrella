"use strict";

$('document').ready(function() {

	$(window).on('action:composer.loaded', function(err, data) {
		// if (data.hasOwnProperty('composerData') && !data.composerData.isMain) {
			// Do nothing, as this is a reply, not a new post
			// return;
		// }

		var btn = $('<button class="btn btn-success composer-evaluate" data-action="evaluate" tabindex="-2"><i class="fa fa-play"></i> 执行脚本</button>');

		btn.on('click', function() {
			var codes = $('#cmp-uuid-' + data.post_uuid + ' .preview.well .language-mma.hljs.mathematica');
			for(var index = 0; index < codes.length; index++) {
				console.log($(codes[index]).text());
			}
		});

		$('#cmp-uuid-' + data.post_uuid + ' .action-bar').prepend(btn);

	});

});