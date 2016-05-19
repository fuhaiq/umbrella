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
$(document).ready(function(){ace.require("ace/ext/language_tools");var d=ace.edit("kernel");d.setTheme("ace/theme/twilight");d.getSession().setMode("ace/mode/mathematica");d.setOptions({enableBasicAutocompletion:true,enableSnippets:false});d.commands.bindKey("alt-q","startAutocomplete");var c=function(e){var f=$.trim(d.getValue());if(!f||f==""){app.alert({title:"消息",message:"没有脚本可以运行",type:"info",timeout:2000});return}f=[f];require(["csrf"],function(g){$.ajax({method:"POST",url:"/kernel",data:{content:JSON.stringify(f)},headers:{"x-csrf-token":g.get()},beforeSend:function(i,h){e.button("loading");d.setReadOnly(true);$("#kernel-preview").empty()}}).done(function(i){if(!i.success){$("#kernel-preview").append('<div class="kernel result alert alert-"'+i.type+' role="alert">'+i.msg+"</div>")}else{var h=JSON.parse(i.result);if(h.length==0){app.alert({title:"消息",message:"没有显示结果",type:"info",timeout:2000})}else{h.forEach(function(j){if(j.type=="text"){$("#kernel-preview").append("<samp>"+j.data+"</samp>")}else{if(j.type=="error"){$("#kernel-preview").append('<div class="kernel result alert alert-danger" role="alert">'+j.data+"</div>")}else{if(j.type=="abort"){$("#kernel-preview").append('<div class="kernel result alert alert-warning" role="alert">计算超时</div>')}else{if(j.type=="image"){$("#kernel-preview").append("<img class='kernel result img-responsive' src='/kernel/temp/"+j.data+"'></img>")}}}}})}}}).always(function(){e.button("reset");d.setReadOnly(false)})})};$("#kernel-evaluate").click(function(){c($(this))});d.commands.bindKey("shift-enter",function(){c($("#kernel-evaluate"))});var a=ajaxify.data.q;if(a){d.setValue(a);c($("#kernel-evaluate"))}var b=ajaxify.data.p;if(b){b.forEach(function(e){d.insert(e)})}});
</script>
