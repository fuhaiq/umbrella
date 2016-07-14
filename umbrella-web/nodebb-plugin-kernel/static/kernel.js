$(document).ready(function() {

  require(["../../plugins/nodebb-plugin-kernel/static/lib/codemirror/lib/codemirror", "../../plugins/nodebb-plugin-kernel/static/lib/codemirror/mode/mathematica/mathematica", "../../plugins/nodebb-plugin-kernel/static/lib/codemirror/addon/edit/matchbrackets"],
  function (CodeMirror) {

    var kernel = CodeMirror.fromTextArea(document.getElementById('kernel'), {
      mode: 'text/x-mathematica',
      lineNumbers: true,
      matchBrackets: true,
      indentWithTabs: true,
      lineWrapping: true,
      theme:'mdn-like'
    });

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
            kernel.setOption("readOnly", true)
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
          kernel.setOption("readOnly", false)
        });
      });
    }

    $('#kernel-evaluate').click(function() {
      evaluate($(this));
    });

    kernel.setOption("extraKeys", {
      'Shift-Enter': function(cm) {
        evaluate($('#kernel-evaluate'));
      }
    });

    var charWidth = kernel.defaultCharWidth(), basePadding = 4;

    kernel.on("renderLine", function(cm, line, elt) {
      var off = CodeMirror.countColumn(line.text, null, cm.getOption("tabSize")) * charWidth;
      elt.style.textIndent = "-" + off + "px";
      elt.style.paddingLeft = (basePadding + off) + "px";
    });

    kernel.refresh();


    var q = ajaxify.data.q;
    if(q) {
      kernel.setValue(q);
      evaluate($('#kernel-evaluate'));
    }

    var p = ajaxify.data.p;
    if(p) {
      kernel.setValue("");
      p.forEach(function(code){
        kernel.replaceRange(code, CodeMirror.Pos(kernel.lastLine()))
      })
    }

  });
})
