<div class="panel panel-default">
  <div class="panel-heading">
    <span class="hidden-xs">
      <kbd><kbd>alt</kbd> + <kbd>q</kbd></kbd>打开语法提示, <kbd><kbd>ctrl</kbd> + <kbd>f</kbd></kbd>代码搜索.
    </span>
    <button type="button" class="btn btn-success btn-sm" data-loading-text="正在计算..." autocomplete="off" id='kernel-evaluate'><span class="hidden-xs"><var>shift</var> + <var>enter</var> =</span> <i class="fa fa-fw fa-play"></i> 执行脚本</button>
  </div>
  <div class="panel-body">
    <div id="kernel" style="width:100%; height:300px"></div>
  </div>
</div>

<div id='kernel-preview'></div>

<script>
$(document).ready(function(){ace.require("ace/ext/language_tools");var a=ace.edit("kernel");a.setTheme("ace/theme/twilight");a.getSession().setMode("ace/mode/mathematica");a.setOptions({enableBasicAutocompletion:!0,enableSnippets:!1,fontSize:"14pt"});a.commands.bindKey("alt-q","startAutocomplete");var d=function(b){var c=$.trim(a.getValue());c&&""!=c?(c=[c],require(["csrf"],function(d){$.ajax({method:"POST",url:"/kernel",data:{content:JSON.stringify(c)},headers:{"x-csrf-token":d.get()},beforeSend:function(e,f){b.button("loading");a.setReadOnly(!0);$("#kernel-preview").empty()}}).done(function(a){a.success?(a=JSON.parse(a.result),0==a.length?app.alert({title:"\u6d88\u606f",message:"\u6ca1\u6709\u663e\u793a\u7ed3\u679c",type:"info",timeout:2E3}):a.forEach(function(a){"text"==a.type?$("#kernel-preview").append("<samp>"+a.data+"</samp>"):"error"==a.type?$("#kernel-preview").append('<div class="kernel result alert alert-danger" role="alert">'+a.data+"</div>"):"abort"==a.type?$("#kernel-preview").append('<div class="kernel result alert alert-warning" role="alert">\u8ba1\u7b97\u8d85\u65f6</div>'):"image"==a.type&&$("#kernel-preview").append("<img class='kernel result img-responsive' src='/kernel/temp/"+a.data+"'></img>")})):$("#kernel-preview").append('<div class="kernel result alert alert-'+a.type+'" role="alert">'+a.msg+"</div>")}).always(function(){b.button("reset");a.setReadOnly(!1)})})):app.alert({title:"\u6d88\u606f",message:"\u6ca1\u6709\u811a\u672c\u53ef\u4ee5\u8fd0\u884c",type:"info",timeout:2E3})};$("#kernel-evaluate").click(function(){d($(this))});a.commands.bindKey("shift-enter",function(){d($("#kernel-evaluate"))});var b=ajaxify.data.q;b&&(a.setValue(b),d($("#kernel-evaluate")));(b=ajaxify.data.p)&&b.forEach(function(b){a.insert(b)})});
</script>
