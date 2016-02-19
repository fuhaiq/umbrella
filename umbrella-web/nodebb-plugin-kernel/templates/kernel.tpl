<style type="text/css" media="screen">
    #kernel {
        width: 100%;
        height: 300px;
    }
</style>

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
$(document).ready(function(){ace.require("ace/ext/language_tools");var b=ace.edit("kernel");b.setTheme("ace/theme/twilight");b.getSession().setMode("ace/mode/mathematica");b.setOptions({enableBasicAutocompletion:true,enableSnippets:false});b.commands.bindKey("alt-q","startAutocomplete");var a=function(c){var d=$.trim(b.getValue());if(!d||d==""){app.alert({title:"消息",message:"没有脚本可以运行",type:"info",timeout:2000});return}d=[d];require(["csrf"],function(e){$.ajax({method:"POST",url:"/kernel",data:{content:JSON.stringify(d)},headers:{"x-csrf-token":e.get()},beforeSend:function(g,f){c.button("loading");b.setReadOnly(true);$("#kernel-preview").empty()}}).done(function(g){if(!g.success){app.alert({title:"消息",message:g.msg,type:g.type,timeout:2000})}else{var f=JSON.parse(g.result);if(f.length==0){app.alert({title:"消息",message:"没有显示结果",type:"info",timeout:2000})}else{f.forEach(function(h){if(h.type=="text"){$("#kernel-preview").append("<samp>"+h.data+"</samp>")}else{if(h.type=="error"){$("#kernel-preview").append('<div class="alert alert-danger" role="alert">'+h.data+"</div>")}else{if(h.type=="abort"){$("#kernel-preview").append('<div class="alert alert-warning" role="alert">计算超时</div>')}else{if(h.type=="image"){$("#kernel-preview").append("<p><img src='/kernel/temp/"+h.data+"'></img></p>")}}}}})}}}).always(function(){c.button("reset");b.setReadOnly(false)})})};$("#kernel-evaluate").click(function(){a($(this))});b.commands.bindKey("shift-enter",function(){a($("#kernel-evaluate"))})});
</script>
