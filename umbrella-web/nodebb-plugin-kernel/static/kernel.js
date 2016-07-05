$(document).ready(function() {

  ace.require("ace/ext/language_tools");
  var kernel = ace.edit("kernel");
  kernel.setTheme("ace/theme/twilight");
  kernel.getSession().setMode('ace/mode/mathematica');
  kernel.setOptions({
      enableBasicAutocompletion: true,
      enableSnippets: false,
      fontSize: "14pt"
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
