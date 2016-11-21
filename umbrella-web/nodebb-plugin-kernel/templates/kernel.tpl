<style type=text/css>
.CodeMirror {border-top: 1px solid black; border-bottom: 1px solid black;font-family: Times, "Times New Roman", Georgia, serif;}
</style>

<div class="panel panel-default">
  <div class="panel-heading">
    <div class="row" id="evaluate-panel">
      <div class="col-xs-6">



		<div class="btn-group">
		  <button type="button" class="btn btn-success btn-sm" data-loading-text="正在计算..." autocomplete="off" id='kernel-evaluate'><span class="hidden-xs"><var>shift</var> + <var>enter</var> =</span> <i class="fa fa-fw fa-play"></i> 执行脚本</button>

		  <div class="btn-group">
			<button class="btn btn-info btn-sm dropdown-toggle" type="button" id="kernel-theme" data-toggle="dropdown">
			主题
			<span class="caret"></span>
			</button>
			<ul class="dropdown-menu" role="menu" aria-labelledby="kernel-theme">
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">default</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">3024-night</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">base16-dark</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">base16-light</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">dracula</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">eclipse</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">elegant</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">erlang-dark</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">midnight</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">monokai</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">the-matrix</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">xq-dark</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">xq-light</a></li>
				<li role="presentation"><a role="menuitem" tabindex="-1" href="#">zenburn</a></li>
			</ul>
		  </div>
		</div>



      </div>
      <div class="col-xs-6" align='right'>
        <span><img src="/3d-24.png"></span> <input type="checkbox" name="kernel-evaluate-3d" data-size="small">
      </div>
    </div>
    <div class="progress" style="display: none;" id="kernel-process"><div class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100" style="width: 100%;">正在计算</div></div>
  </div>
  <div class="panel-body">
    <textarea id="kernel">(* Mathematica code *)</textarea>
  </div>
</div>

<div id='kernel-preview'></div>
<script>
$(document).ready(function() {

  require(["../../plugins/nodebb-plugin-kernel/static/lib/codemirror/lib/codemirror", "../../plugins/nodebb-plugin-kernel/static/lib/codemirror/mode/mathematica/mathematica", "../../plugins/nodebb-plugin-kernel/static/lib/codemirror/addon/edit/matchbrackets"],
  function (CodeMirror) {

    $("[name='kernel-evaluate-3d']").bootstrapSwitch();

    var kernel = CodeMirror.fromTextArea(document.getElementById('kernel'), {
      mode: 'text/x-mathematica',
      lineNumbers: true,
      matchBrackets: true,
      indentWithTabs: true,
      lineWrapping: true,
      theme:'zenburn'
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

      var enable3d = $("[name='kernel-evaluate-3d']").bootstrapSwitch('state')

        $.ajax({
          method: 'POST',
          url: "/kernel",
          data: {
            content : JSON.stringify(content),
            enable3d : enable3d
          },
          headers: {
            'x-csrf-token': config.csrf_token
          },
          beforeSend: function(xhr, settings) {
            btn.button('loading');
            $('#evaluate-panel').hide();
            $('#kernel-process').show();
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
                }else if(item.type == "x3d") {
                  $('#kernel-preview').append("<x3d width='300px' height='300px'><scene><inline url='/kernel/temp/"+item.data+".x3d'></inline></scene></x3d>")
                }
              });
              x3dom.reload();
            }
          }
        })
        .always(function() {
          $('#kernel-process').hide();
          btn.button('reset');
          $('#evaluate-panel').show();
          kernel.setOption("readOnly", false)
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

    var d = ajaxify.data.d;
    if(d) {
      $("[name='kernel-evaluate-3d']").bootstrapSwitch('state', true)
    }

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

	$("ul.dropdown-menu > li a").click(function(){
		var me = $(this)
		var theme = me.text()
		kernel.setOption("theme", theme);
		$("#kernel-theme").text(theme);
	})

  });
})

</script>
