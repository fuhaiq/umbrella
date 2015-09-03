"use strict";

$('document').ready(function() {

	$(window).on('action:composer.loaded', function(err, data) {

		var btn = $('<button class="btn btn-success composer-evaluate" data-action="evaluate" tabindex="-2"><i class="fa fa-play"></i> 执行脚本</button>');
		
		var showBtn = $('#cmp-uuid-' + data.post_uuid).find('.write-container .toggle-preview');

		var editor = $('#cmp-uuid-' + data.post_uuid).find('textarea');

		btn.on('click', function() {
			$('#cmp-uuid-' + data.post_uuid + ' .preview.well .kernel.result').remove();
			showBtn.click();
			var scripts = [];
			var codes = $('#cmp-uuid-' + data.post_uuid + ' .preview.well .language-mma.hljs.mathematica');
			for(var index = 0; index < codes.length; index++) {
				scripts.push($(codes[index]).text());
			}
			if(scripts.length > 0) {

				require(['csrf'], function(csrf) {
	                $.ajax({
	                    method: 'POST',
	                    url: "/kernel",
	                    data: {
	                    	content : JSON.stringify(scripts)
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
	                    if(!json.success) {
	                        app.alert({
	                            title: '消息',
	                            message: json.msg,
	                            type: json.type,
	                            timeout: 2000
	                        });
	                    } else {
	                        var data = JSON.parse(json.data);
	                        data.forEach(function(item){
	                        	if(item.type == 'return' || item.type == 'text') {
	                                $(codes[item.index]).after('<div class="kernel result alert alert-success" role="alert">'+item.data+'</div>');
	                                MathJax.Hub.Config({
	                                    "HTML-CSS": { linebreaks: { automatic: true } },
	                                    SVG: { linebreaks: { automatic: true } }
	                                });
	                                MathJax.Hub.Queue(["Typeset", MathJax.Hub, '#cmp-uuid-' + data.post_uuid]);
	                            }else if(item.type == "error") {
	                                $(codes[item.index]).after('<div class="kernel result alert alert-danger" role="alert">'+item.data+'</div>')
	                            }else if(item.type == "abort") {
	                                $(codes[item.index]).after('<div class="kernel result alert alert-warning" role="alert">运行超时</div>')
	                            }else if(item.type == "image") {
	                                $(codes[item.index]).after("<img class='kernel result' src='/kernel/temp/"+item.data+"''></img>")
	                            }
	                        });
	                    }
	                })
	                .fail(function() {
	                    app.alertError('Mathematica服务目前不可用');
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
	});


	$(window).on('action:topic.loaded', function(event) {
		MathJax.Hub.Config({
            "HTML-CSS": { linebreaks: { automatic: true } },
            SVG: { linebreaks: { automatic: true } }
        });
        MathJax.Hub.Queue(["Typeset", MathJax.Hub, 'content']);
	});

	socket.on('event:umbrella', function (json){
		console.log(json)
	});
});