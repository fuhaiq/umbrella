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
$(document).ready(function(){ace.require("ace/ext/language_tools");var c=ace.edit("kernel");c.setTheme("ace/theme/twilight");c.getSession().setMode("ace/mode/mathematica");c.setOptions({enableBasicAutocompletion:true,enableSnippets:false});c.commands.bindKey("alt-q","startAutocomplete");var b=function(d){var e=$.trim(c.getValue());if(!e||e==""){app.alert({title:"消息",message:"没有脚本可以运行",type:"info",timeout:2000});return}e=[e];require(["csrf"],function(f){$.ajax({method:"POST",url:"/kernel",data:{content:JSON.stringify(e)},headers:{"x-csrf-token":f.get()},beforeSend:function(h,g){d.button("loading");c.setReadOnly(true);$("#kernel-preview").empty()}}).done(function(h){if(!h.success){app.alert({title:"消息",message:h.msg,type:h.type,timeout:2000})}else{var g=JSON.parse(h.result);if(g.length==0){app.alert({title:"消息",message:"没有显示结果",type:"info",timeout:2000})}else{g.forEach(function(i){if(i.type=="text"){$("#kernel-preview").append("<samp>"+i.data+"</samp>")}else{if(i.type=="error"){$("#kernel-preview").append('<div class="kernel result alert alert-danger" role="alert">'+i.data+"</div>")}else{if(i.type=="abort"){$("#kernel-preview").append('<div class="kernel result alert alert-warning" role="alert">计算超时</div>')}else{if(i.type=="image"){$("#kernel-preview").append("<img class='kernel result img-responsive' src='/kernel/temp/"+i.data+"'></img>")}}}}})}}}).always(function(){d.button("reset");c.setReadOnly(false)})})};$("#kernel-evaluate").click(function(){b($(this))});c.commands.bindKey("shift-enter",function(){b($("#kernel-evaluate"))});var a=ajaxify.data.q;if(a){c.setValue(a);b($("#kernel-evaluate"))}});
</script>
