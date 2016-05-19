/node_modules/nodebb-theme-persona/templates/partials/notifications_list.tpl
<!-- IF notifications.from -->
<a href="{config.relative_path}/user/{notifications.user.userslug}"><img src="{notifications.image}" /></a>
<!-- ELSE -->
<a href="{config.relative_path}{notifications.path}"><img src="{notifications.image}" /></a>
<!-- ENDIF notifications.from -->

/node_modules/nodebb-theme-persona/templates/partials/topic/post.tpl
<!-- IF posts.waiting -->
<span class="label label-info"><i class="fa fa-clock-o"></i> 等待运算</span>
<!-- ENDIF posts.waiting -->
<!-- IF posts.evaluate -->
<span class="label label-primary"><i class="fa fa-play"></i> 正在计算</span>
<!-- ENDIF posts.evaluate -->
<!-- IF posts.finished -->
<span class="label label-success"><i class="fa fa-check"></i> 计算成功</span>
<a class="btn btn-default btn-xs" href="/kernel?p={posts.pid}" role="button"><i class="fa fa-play" aria-hidden="true"></i> 加载到执行器</a>
<!-- ENDIF posts.finished -->
<!-- IF posts.error -->
<span class="label label-danger"><i class="fa fa-remove"></i> 语法错误</span>
<a class="btn btn-default btn-xs" href="/kernel?p={posts.pid}" role="button"><i class="fa fa-play" aria-hidden="true"></i> 加载到执行器</a>
<!-- ENDIF posts.error -->
<!-- IF posts.aborted -->
<span class="label label-warning"><i class="fa fa-exclamation"></i> 计算超时</span>
<a class="btn btn-default btn-xs" href="/kernel?p={posts.pid}" role="button"><i class="fa fa-play" aria-hidden="true"></i> 加载到执行器</a>
<!-- ENDIF posts.aborted -->



/node_modules/nodebb-theme-persona/templates/partials/topics_list.tpl
<small class="label label-status">
<!-- IF topics.waiting -->
<span class="label label-info"><i class="fa fa-clock-o"></i> 等待运算</span>
<!-- ENDIF topics.waiting -->
<!-- IF topics.evaluate -->
<span class="label label-primary"><i class="fa fa-play"></i> 正在计算</span>
<!-- ENDIF topics.evaluate -->
<!-- IF topics.finished -->
<span class="label label-success"><i class="fa fa-check"></i> 计算成功</span>
<!-- ENDIF topics.finished -->
<!-- IF topics.error -->
<span class="label label-danger"><i class="fa fa-remove"></i> 语法错误</span>
<!-- ENDIF topics.error -->
<!-- IF topics.aborted -->
<span class="label label-warning"><i class="fa fa-exclamation"></i> 计算超时</span>
<!-- ENDIF topics.aborted -->
</small>


######################kernel.js#########################

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
                  $('#kernel-preview').append('<div class="kernel result alert alert-'+json.type+'" role="alert">'+json.msg+'</div>')
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
                              $('#kernel-preview').append('<div class="kernel result alert alert-danger" role="alert">'+item.data+'</div>')
                          }else if(item.type == "abort") {
                              $('#kernel-preview').append('<div class="kernel result alert alert-warning" role="alert">计算超时</div>')
                          }else if(item.type == "image") {
                              $('#kernel-preview').append("<img class='kernel result img-responsive' src='/kernel/temp/"+item.data+"'></img>")
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

  var q = ajaxify.data.q;
  if(q) {
    kernel.setValue(q);
    evaluate($('#kernel-evaluate'));
  }

  var p = ajaxify.data.p;
  if(p) {
    p.forEach(function(code){
      kernel.insert(code);
    })
  }
})
