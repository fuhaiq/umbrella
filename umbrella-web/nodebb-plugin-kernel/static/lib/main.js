"use strict";

$('document').ready(function() {



	$(window).on('action:app.load', function() {

		require(['components'], function(components) {

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
			                        if(data.length == 0) {
			                            app.alert({
			                                title: '消息',
			                                message: '没有显示结果',
			                                type: 'info',
			                                timeout: 2000
			                            });
			                        } else {
			                        	data.forEach(function(item){
				                        	if(item.type == 'return' || item.type == 'text') {
				                                $(codes[item.index]).after('<div class="kernel result alert alert-success" role="alert">'+item.data+'</div>');
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
			});


			$(window).on('action:topic.loaded', function(event) {
		        MathJax.Hub.Queue(["Typeset", MathJax.Hub, 'content']);
			});

			socket.on('kernel:topic', function (json){
				var li = components.get('category/topic', 'tid', json.tid);
				if(li && li.length) {
					var title = $('[component="topic/header"] > a', li);
					if(title && title.length) {
						title = title[0];
						$(title).find("span:first-child").remove(); //always remove the span first, this will handle status = 0
						if(json.status == 1) {
							$(title).prepend('<span class="kernel waiting"><i class="fa fa-clock-o"></i> 等待运算</span>');
						} else if(json.status == 2) {
							$(title).prepend('<span class="kernel evaluate"><i class="fa fa-play"></i> 正在计算</span>');
						} else if(json.status == 3) {
							$(title).prepend('<span class="kernel finished"><i class="fa fa-check"></i> 计算完成</span>');
						} else if(json.status == -1) {
							$(title).prepend('<span class="kernel error"><i class="fa fa-remove"></i> 语法错误</span>');
						} else if(json.status == -2) {
							$(title).prepend('<span class="kernel aborted"><i class="fa fa-exclamation"></i> 计算超时</span>');
						}
					}
				}
			});

			socket.on('kernel:post', function (json) {
				var li = components.get('post', 'pid', json.pid);
				if(li && li.length) {
					//Add status span
					var span = $('span.kernel', li)
					if(span && span.length) {
						span = span[0];
						if(json.status == 1) {
							$(span).after('<span class="kernel waiting"><i class="fa fa-clock-o"></i> 等待运算</span>');
						} else if(json.status == 2) {
							$(span).after('<span class="kernel evaluate"><i class="fa fa-play"></i> 正在计算</span>');
						} else if(json.status == 3) {
							$(span).after('<span class="kernel finished"><i class="fa fa-check"></i> 计算完成</span>');
						} else if(json.status == -1) {
							$(span).after('<span class="kernel error"><i class="fa fa-remove"></i> 语法错误</span>');
						} else if(json.status == -2) {
							$(span).after('<span class="kernel aborted"><i class="fa fa-exclamation"></i> 计算超时</span>');
						}
						$(span).remove();
					} else {
						var span = $('.post-tools', li);
						if(span && span.length) {
							span = span[0];
							if(json.status == 1) {
								$(span).after('<span class="kernel waiting"><i class="fa fa-clock-o"></i> 等待运算</span>');
							} else if(json.status == 2) {
								$(span).after('<span class="kernel evaluate"><i class="fa fa-play"></i> 正在计算</span>');
							} else if(json.status == 3) {
								$(span).after('<span class="kernel finished"><i class="fa fa-check"></i> 计算完成</span>');
							} else if(json.status == -1) {
								$(span).after('<span class="kernel error"><i class="fa fa-remove"></i> 语法错误</span>');
							} else if(json.status == -2) {
								$(span).after('<span class="kernel aborted"><i class="fa fa-exclamation"></i> 计算超时</span>');
							}
						}
					}
					//Add result mathjax
					if(json.status == 3 || json.status == -1 || json.status == -2) {
						var codes = $('code.language-mma', li);
						if(codes && codes.length) {
							json.data.forEach(function (item){
								var id = 'socket-io-kernel-post' + json.pid + '-' + item.index
								if(item.type == 'return' || item.type == 'text') {
	                                $(codes[item.index]).after('<div id="'+id+'" class="kernel result alert alert-success" role="alert">'+item.data+'</div>');
	                                MathJax.Hub.Queue(["Typeset", MathJax.Hub, id]);
	                            }else if(item.type == "error") {
	                                $(codes[item.index]).after('<div class="kernel result alert alert-danger" role="alert">'+item.data+'</div>')
	                            }else if(item.type == "abort") {
	                                $(codes[item.index]).after('<div class="kernel result alert alert-warning" role="alert">运行超时</div>')
	                            }else if(item.type == "image") {
	                                $(codes[item.index]).after("<img class='kernel result' src='/kernel/"+json.pid+"/"+item.data+"''></img>")
	                            }
							});
						}
					}
					
				}
			});



		});
	});

	
});