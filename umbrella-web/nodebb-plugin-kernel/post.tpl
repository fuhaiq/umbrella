<!-- IF posts.waiting -->
<span class="waiting"><i class="fa fa-clock-o"></i> 等待运算</span>
<!-- ENDIF posts.waiting -->
<!-- IF posts.evaluate -->
<span class="evaluate"><i class="fa fa-play"></i> 正在计算</span>
<!-- ENDIF posts.evaluate -->
<!-- IF posts.finished -->
<span class="finished"><i class="fa fa-check"></i> 计算完成</span>
<!-- ENDIF posts.finished -->
<!-- IF posts.error -->
<span class="error"><i class="fa fa-remove"></i> 语法错误</span>
<!-- ENDIF posts.error -->
<!-- IF posts.aborted -->
<span class="aborted"><i class="fa fa-exclamation"></i> 计算超时</span>
<!-- ENDIF posts.aborted -->