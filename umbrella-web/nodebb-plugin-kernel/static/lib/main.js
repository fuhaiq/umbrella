"use strict";

var statusMapping = {
	waiting: '<span class="ui blue basic label"><i class="fa fa-clock-o"></i> 等待计算</span>',
	evaluate: '<span class="ui purple basic label"><i class="fa fa-play"></i> 正在计算</span>',
	finished: '<span class="ui green basic label"><i class="fa fa-check"></i> 计算成功</span>',
	error: '<span class="ui orange basic label"><i class="fa fa-remove"></i> 语法错误</span>',
	aborted: '<span class="ui yellow basic label"><i class="fa fa-exclamation"></i> 计算超时</span>'
};

$('document').ready(function() {
	$(window).on('action:app.load', function() {

		require(['composer/formatting', 'composer/controls', 'components'], function(formatting, controls, components) {

			socket.on('kernel:topic', function(json) {
				var topic = components.get('category/topic', 'tid', json.tid);
				if (!topic || !topic.length) {
					return;
				}
				var title = $('[component="topic/header"] > a', topic);
				if (!title || !title.length) {
					return;
				}
				title = title[0];
				$(title).find("span:first-child").remove(); //always remove the span first, this will handle status = 0
				switch (json.status) {
					case 1:
						$(title).prepend(statusMapping.waiting);
						break;
					case 2:
						$(title).prepend(statusMapping.evaluate);
						break;
					case 3:
						$(title).prepend(statusMapping.finished);
						break;
					case -1:
						$(title).prepend(statusMapping.error);
						break;
					case -2:
						$(title).prepend(statusMapping.aborted);
						break;
				}
			});

			$(window).on('action:composer.loaded', function(err, data) {
				//Add dispatch button
				formatting.addButton('code', function(textarea, selectionStart, selectionEnd){
					if (selectionStart === selectionEnd) {
							controls.insertIntoTextarea(textarea, '````mma\n(*mathematica code*)\n````');
							controls.updateTextareaSelection(textarea, selectionStart + 8, selectionStart + 28);
						} else {
							controls.wrapSelectionInTextareaWith(textarea, '````mma\n', '\n````');
							controls.updateTextareaSelection(textarea, selectionStart + 8, selectionEnd + 8);
						}
				});
				//Add kernel button
				var btn = $('<button class="btn btn-success composer-evaluate" data-action="evaluate" tabindex="-2"><i class="fa fa-play"></i> 执行</button>');
				var showBtn = $('#cmp-uuid-' + data.post_uuid).find('.write-container .toggle-preview');
				var editor = $('#cmp-uuid-' + data.post_uuid).find('textarea');
				btn.on('click', function() {
					$('#cmp-uuid-' + data.post_uuid + ' .preview.well .kernel.result').remove();
					showBtn.click();
					var scripts = [];
					var codes = $('#cmp-uuid-' + data.post_uuid + ' .preview.well .language-mma.hljs.mathematica');
					for (var index = 0; index < codes.length; index++) {
						scripts.push($(codes[index]).text());
					}
					if (scripts.length > 0) {
						require(['csrf'], function(csrf) {
							$.ajax({
									method: 'POST',
									url: "/kernel",
									data: {
										content: JSON.stringify(scripts)
									},
									headers: {
										'x-csrf-token': csrf.get()
									},
									beforeSend: function(xhr, settings) {
										editor.attr('disabled', true);
										btn.button('loading');
									}
								})
								.done(function(json) {
									if (!json.success) {
										app.alert({
											title: '消息',
											message: json.msg,
											type: json.type,
											timeout: 2000
										});
									} else {
										var result = JSON.parse(json.result);
										if (result.length == 0) {
											app.alert({
												title: '消息',
												message: '没有显示结果',
												type: 'info',
												timeout: 2000
											});
										} else {
											result.forEach(function(item) {
												if(item.type == "text") {
													$(codes[item.index]).parent().append('<samp>' + item.data + '</samp>')
												} else if (item.type == "error") {
													$(codes[item.index]).parent().append('<div class="kernel result alert alert-danger" role="alert">' + item.data + '</div>')
												} else if (item.type == "abort") {
													$(codes[item.index]).parent().append('<div class="kernel result alert alert-warning" role="alert">计算超时</div>')
												} else if (item.type == "image") {
													$(codes[item.index]).parent().append("<img class='kernel result img-responsive' src='/kernel/temp/" + item.data + "'></img>")
												}
											});
										}
									}
								})
								.always(function() {
									editor.attr('disabled', false);
									btn.button('reset');
								});
						});
					} else {
						app.alert({
							type: 'info',
							title: '消息',
							message: '没有脚本可以运行',
							timeout: 2000
						});
					}
				});
				$('#cmp-uuid-' + data.post_uuid + ' .action-bar').prepend(btn);
				// if (data.hasOwnProperty('composerData') && !data.composerData.isMain) {
				// Do nothing, as this is a reply, not a new post
				// return;
				// }

			}); //end of action:composer.loaded
		}); // end of require(['components']
	}); // end of action:app.load
}); // end of $('document').ready
