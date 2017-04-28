<style type=text/css>
.kernel-area {
  border-right-width:5px;
  border-right-color:#1b809e;
  margin-bottom:5px;
  padding-bottom: 5px;
}
.CodeMirror {
  height: auto;
  font-family: Times, "Times New Roman", Georgia, serif;
}
.CodeMirror-scroll {
  height: auto;
  overflow-y: hidden;
  overflow-x: auto;
}
</style>
<div class="well well-sm">
  <button type="button" class="btn btn-success btn-sm btn-block" data-loading-text="正在计算..." autocomplete="off" id='kernel-evaluate'><span class="hidden-xs"><var>shift</var> + <var>enter</var> =</span> <i class="fa fa-fw fa-play"></i> 执行脚本</button>
  <div class="progress" style="display: none;" id="kernel-process"><div class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100" style="width: 100%;">正在计算</div></div>
</div>
<div id="kernel"></div>

<script>
$(document).ready(function() {
  require(["../../plugins/nodebb-plugin-kernel/static/lib/codemirror/lib/codemirror", "../../plugins/nodebb-plugin-kernel/static/lib/codemirror/mode/mathematica/mathematica", "../../plugins/nodebb-plugin-kernel/static/lib/codemirror/addon/edit/matchbrackets"],
  function (CodeMirror) {

    var createArea = function(init) {
      var area = $('<div></div>', {
        "class": "kernel-area alert alert-dismissible fade in"
      })
      var area_input = $('<div></div>',{
        "class": "kernel-area-input"
      })
      var area_output = $('<div></div>',{
        "class": "kernel-area-output"
      })
      $(area).on('close.bs.alert', function () {
        if($(area).is(':only-child')) {
          kernel.setValue('')
          $(area_output).empty();
          return false
        }
        return true
      })
      var textarea = document.createElement('textarea')
      $(area_input).append(textarea)
      $(area).append('<a class="close" data-dismiss="alert" aria-label="close">&times;</a>')
      $(area).append(area_input)
      $(area).append(area_output)
      $('#kernel').append(area)

      var kernel = CodeMirror.fromTextArea(textarea, {
        mode: 'text/x-mathematica',
        lineNumbers: true,
        matchBrackets: true,
        indentWithTabs: true,
        lineWrapping: true,
        theme:'zenburn'
      });

      if(init && $.trim(init) != '') {
        kernel.setValue(init)
      }

      kernel.setOption("extraKeys", {
        'Shift-Enter': function(cm) {
          evaluate(area)
        }
      });
      var charWidth = kernel.defaultCharWidth(), basePadding = 4;

      kernel.on("renderLine", function(cm, line, elt) {
        var off = CodeMirror.countColumn(line.text, null, cm.getOption("tabSize")) * charWidth;
        elt.style.textIndent = "-" + off + "px";
        elt.style.paddingLeft = (basePadding + off) + "px";
      });

      kernel.refresh();
      return area;
    }// end of createArea

    var evaluate = function(area) {
      var content = getEvaluateValue(area)
      var empty = true
      for(var index = 0; index < content.length; index++) {
        if(content[index] != '') {
          empty = false
          break;
        }
      }
      if(empty){
        app.alert({title: '消息', message: '没有脚本可以运行', type: 'info', timeout: 2000});
        return
      }

      var outputs = getEvaluateOutput(area);
      var allEditor = getAllEditor();
      var btn = $('#kernel-evaluate');

      $.ajax({
        method: 'POST',
        url: "/kernel",
        data: {
          content : JSON.stringify(content)
        },
        headers: {
          'x-csrf-token': config.csrf_token
        },
        beforeSend: function(xhr, settings) {
          btn.button('loading');
          btn.hide();
          $('#kernel-process').show();
          outputs.forEach(function(output){
            $(output).empty();
          })
          allEditor.forEach(function(editor){
            editor.setOption("readOnly", true)
          })
        }
      })
      .done(function(json) {
        if(!json.success) {
          app.alert({title: '消息', message: json.msg, type: json.type, timeout: 2000});
        } else {
          var result = JSON.parse(json.result);
          if(result.length == 0) {
            app.alert({title: '消息', message: '没有显示结果', type: 'info', timeout: 2000});
          } else {
            result.forEach(function(item){
              var output = outputs[item.index]
              if (item.type == 'text') {
                $(output).append('<samp>' + item.data + '</samp>');
              }else if(item.type == "error") {
                $(output).append('<div class="kernel result alert alert-danger" role="alert">'+item.data+'</div>')
              }else if(item.type == "abort") {
                $(output).append('<div class="kernel result alert alert-warning" role="alert">计算超时</div>')
              }else if(item.type == "image") {
                $(output).append("<img class='kernel result img-responsive' src='/assets/kernel/temp/"+item.data+"'></img>")
              }
            })

            if($(area).is(':last-child')) {
              var editor = getEditor(area)
              if(editor && $.trim(editor.getValue()) != ''){
                createArea();
              }
            }
          }
        }
      })
      .always(function() {
        $('#kernel-process').hide();
        btn.button('reset');
        btn.show();
        allEditor.forEach(function(editor){
          editor.setOption("readOnly", false)
        })
      });
    }// end of evaluate

    var getEvaluateOutput = function(area) {
      var outputs = [];
      var prevArea = $(area).prevAll();
      for(var index = 0; index < prevArea.length; index++) {
        var output = $(prevArea[index]).find('.kernel-area-output')
        if(output && output.length && output.length > 0) outputs.push(output[0])
      }
      outputs.reverse()

      var output = $(area).find('.kernel-area-output')
      if(output && output.length && output.length > 0) outputs.push(output[0])
      return outputs
    }// end of getEvaluateOutput


    var getEvaluateValue = function(area) {
      var content = [];
      var prevArea = $(area).prevAll();
      for(var index = 0; index < prevArea.length; index++) {
        var editor = getEditor(prevArea[index])
        if(editor) {
          var value = $.trim(editor.getValue())
          // if(value == '') value = '\n'
          content.push(value)
        }
      }
      content.reverse()

      var editor = getEditor(area)
      if(editor) content.push($.trim(editor.getValue()))
      return content;
    }// end of getEvaluateValue

    var getEditor = function(area){
      var editor = $(area).find('.CodeMirror')
      if(editor && editor.length && editor.length > 0) {
        return editor[0].CodeMirror
      }
      return null
    }// end of getEditor

    var getAllEditor = function(){
      var editors = [];
      var selector = $('.CodeMirror')
      if(selector && selector.length) {
        for(var index = 0; index < selector.length; index++) {
          editors.push(selector[index].CodeMirror)
        }
      }
      return editors
    }// end of getAllEditor

    $('#kernel-evaluate').click(function() {
      var last = $('.kernel-area:last')
      if(last && last.length && last.length > 0) {
        evaluate(last[0]);
      }
    });

    $('#kernel').empty();

    var q = null;
    if(ajaxify && ajaxify.data && ajaxify.data.q) q = ajaxify.data.q;

    var p = null;
    if(ajaxify && ajaxify.data && ajaxify.data.p) p = ajaxify.data.p;

    if(q) {
      createArea(q);
      $("#kernel-evaluate").trigger( "click" );
    } else if (p) {
      p.forEach(function(code){
        createArea(code);
      })
    } else {
      var firstEditor = getEditor(createArea())
      firstEditor.execCommand('newlineAndIndent')
      firstEditor.execCommand('newlineAndIndent')
    }
  });//end of require
})
</script>
