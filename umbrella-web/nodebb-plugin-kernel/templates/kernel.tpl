<style type="text/css" media="screen">
    #kernel { 
        width: 100%;
        height: 300px;
    }
</style>

<div class="panel panel-default">
  <div class="panel-heading"><button type="button" class="btn btn-primary" data-loading-text="正在运行..." autocomplete="off" id='kernel-evaluate'><i class="fa fa-fw fa-play"></i> 运行</button></div>
  <div class="panel-body">
    <div id="kernel"></div>
  </div>
</div>

<div class="panel panel-default">
  <div class="panel-heading">执行结果</div>
  <div class="panel-body" id='kernel-preview'></div>
</div>

<script>
    $(document).ready(function() {
        var kernel = ace.edit("kernel");
	    kernel.setTheme("ace/theme/twilight");
	    kernel.getSession().setMode('ace/mode/mathematica');

        $('#kernel-evaluate').click(function(){

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

            var $btn = $(this);

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
                        $btn.button('loading');
                        kernel.setReadOnly(true);
                        $('#kernel-preview').empty();
                    }
                })
                .done(function(json) {
                    if(!json.success) {
                        app.alert({
                            title: '消息',
                            message: json.msg,
                            type: 'info',
                            timeout: 2000
                        });
                    } else {
                        var data = JSON.parse(json.data);
                        data.forEach(function(item){
                            if(item.type == 'return' || item.type == 'text') {
                                $('#kernel-preview').append('<div class="alert alert-success" role="alert">'+item.data+'</div>');
                                MathJax.Hub.Config({
                                    "HTML-CSS": { linebreaks: { automatic: true } },
                                    SVG: { linebreaks: { automatic: true } }
                                });
                                MathJax.Hub.Queue(["Typeset",MathJax.Hub,"kernel-preview"]);
                            }else if(item.type == "error") {
                                $('#kernel-preview').append('<div class="alert alert-danger" role="alert">'+item.data+'</div>')
                            }else if(item.type == "abort") {
                                $('#kernel-preview').append('<div class="alert alert-warning" role="alert">运行超时</div>')
                            }else if(item.type == "image") {
                                $('#kernel-preview').append("<img src='/kernel/temp/"+item.data+"''></img>")
                            }
                        });
                        
                    }
                })
                .fail(function() {
                    app.alertError('Mathematica服务目前不可用');
                })
                .always(function() {
                    $btn.button('reset');
                    kernel.setReadOnly(false);
                });
            });
            
        });
    })
</script>