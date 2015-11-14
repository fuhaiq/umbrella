/node_modules/nodebb-theme-persona/templates/partials/topic/post.tpl


<!-- IF posts.waiting -->
<span class="kernel waiting"><i class="fa fa-clock-o"></i> 等待运算</span>
<!-- ENDIF posts.waiting -->
<!-- IF posts.evaluate -->
<span class="kernel evaluate"><i class="fa fa-play"></i> 正在计算</span>
<!-- ENDIF posts.evaluate -->
<!-- IF posts.finished -->
<span class="kernel finished"><i class="fa fa-check"></i> 计算完成</span>
<!-- ENDIF posts.finished -->
<!-- IF posts.error -->
<span class="kernel error"><i class="fa fa-remove"></i> 语法错误</span>
<!-- ENDIF posts.error -->
<!-- IF posts.aborted -->
<span class="kernel aborted"><i class="fa fa-exclamation"></i> 计算超时</span>
<!-- ENDIF posts.aborted -->