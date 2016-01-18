<style type="text/css" media="screen">
    #kernel {
        width: 100%;
        height: 300px;
    }
</style>
<script src="/vendor/ace/ext-language_tools.js"></script>

<div class="panel panel-default">
  <div class="panel-heading">
    <kbd><kbd>alt</kbd> + <kbd>q</kbd></kbd>打开语法提示, <kbd><kbd>ctrl</kbd> + <kbd>f</kbd></kbd>代码搜索.
    <button type="button" class="btn btn-success btn-xs" data-loading-text="正在计算..." autocomplete="off" id='kernel-evaluate'><i class="fa fa-fw fa-play"></i> <var>shift</var> + <var>enter</var> = 执行脚本</button>
  </div>
  <div class="panel-body">
    <div id="kernel"></div>
  </div>
</div>

<div id='kernel-preview'></div>

<script>
    $(document).ready(function() {
        ace.require("ace/ext/language_tools");
        var kernel = ace.edit("kernel");
        kernel.setTheme("ace/theme/twilight");
        kernel.getSession().setMode('ace/mode/mathematica');
        kernel.setOptions({
            enableBasicAutocompletion: true,
            enableSnippets: false
        });
        kernel.commands.bindKey("alt-q", "startAutocomplete");

        var evaluate = function (btn) {
            var content = $.trim(kernel.getValue());

            if(!content || content == '') {
                app.alert({
                    title: '消息',
                    message: '没有脚本可以运行',
                    type: 'info',
                    timeout: 2000
                });
                return;
            }
            content = [content];

            require(['csrf'], function(csrf) {
                $.ajax({
                    method: 'POST',
                    url: "/kernel",
                    data: {
                        content : JSON.stringify(content)
                    },
                    headers: {
                        'x-csrf-token': csrf.get()
                    },
                    beforeSend: function(xhr, settings) {
                        btn.button('loading');
                        kernel.setReadOnly(true);
                        $('#kernel-preview').empty();
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
                        var result = JSON.parse(json.result);
                        if(result.length == 0) {
                            app.alert({
                                title: '消息',
                                message: '没有显示结果',
                                type: 'info',
                                timeout: 2000
                            });
                        } else {
                            result.forEach(function(item){
                                if (item.type == 'text') {
                                    $('#kernel-preview').append('<samp>' + item.data + '</samp>');
                                }else if(item.type == "error") {
                                    $('#kernel-preview').append('<div class="alert alert-danger" role="alert">'+item.data+'</div>')
                                }else if(item.type == "abort") {
                                    $('#kernel-preview').append('<div class="alert alert-warning" role="alert">计算超时</div>')
                                }else if(item.type == "image") {
                                    $('#kernel-preview').append("<p><img src='/kernel/temp/"+item.data+"'></img></p>")
                                }
                            });
                        }
                    }
                })
                .always(function() {
                    btn.button('reset');
                    kernel.setReadOnly(false);
                });
            });
        }

        $('#kernel-evaluate').click(function() {
            evaluate($(this));
        });

        kernel.commands.bindKey("shift-enter", function() {
            evaluate($('#kernel-evaluate'));
        });
    })
</script>
