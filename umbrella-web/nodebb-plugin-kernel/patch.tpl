
/node_modules/nodebb-plugin-recent-cards/static/templates/partials/nodebb-plugin-recent-cards/header.tpl
<ul class="categories">
	<p>[[kernel:recent_topic]]</p>
</ul>

/node_modules/nodebb-theme-persona/templates/partials/topic/post.tpl
<!-- IF posts.waiting -->
<span class="ui blue basic label">
  <i class="fa fa-clock-o"></i>
  等待运算
</span>
<!-- ENDIF posts.waiting -->
<!-- IF posts.evaluate -->
<span class="ui purple basic label">
  <i class="fa fa-play"></i>
  正在计算
</span>
<!-- ENDIF posts.evaluate -->
<!-- IF posts.finished -->
<span class="ui green basic label">
  <i class="fa fa-check"></i>
  计算成功
</span>
<!-- ENDIF posts.finished -->
<!-- IF posts.error -->
<span class="ui orange basic label">
  <i class="fa fa-remove"></i>
  语法错误
</span>
<!-- ENDIF posts.error -->
<!-- IF posts.aborted -->
<span class="ui yellow basic label">
  <i class="fa fa-exclamation"></i>
  计算超时
</span>
<!-- ENDIF posts.aborted -->



/node_modules/nodebb-theme-persona/templates/partials/topic_list.tpl
<!-- IF topics.waiting -->
<span class="ui blue basic label">
  <i class="fa fa-clock-o"></i>
  等待运算
</span>
<!-- ENDIF topics.waiting -->
<!-- IF topics.evaluate -->
<span class="ui purple basic label">
  <i class="fa fa-play"></i>
  正在计算
</span>
<!-- ENDIF topics.evaluate -->
<!-- IF topics.finished -->
<span class="ui green basic label">
  <i class="fa fa-check"></i>
  计算成功
</span>
<!-- ENDIF topics.finished -->
<!-- IF topics.error -->
<span class="ui orange basic label">
  <i class="fa fa-remove"></i>
  语法错误
</span>
<!-- ENDIF topics.error -->
<!-- IF topics.aborted -->
<span class="ui yellow basic label">
  <i class="fa fa-exclamation"></i>
  计算超时
</span>
<!-- ENDIF topics.aborted -->
