<div class="row">

  <div class="col-lg-9">
    <form action="/kernel" method="get" id="kernel-quick-form">
      <div class="input-group has-success input-group-sm">
        <span class="input-group-addon" id="basic-addon1"><i class="fa fa-code" aria-hidden="true"></i></span>
        <input type="text" name="q" class="form-control" placeholder="执行" maxlength="100" id="kernel-quick-input">
        <span class="input-group-btn">
          <button class="btn btn-success" type="submit"><i class="fa fa-play" aria-hidden="true"></i></button>
        </span>
      </div>
    </form>
    <br>
    <!-- Categories Original content -->
  </div>

  <div class="col-lg-3">
    <!-- IF kernelPosts.length -->
    <ul class="categories" itemscope="" itemtype="http://www.schema.org/ItemList">
      <!-- BEGIN kernelPosts -->
      <li component="post" data-pid="{kernelPosts.pid}" class="row clearfix">
        <div class="card" style="border-color: {kernelPosts.category.bgColor};">
          <div component="post">
            <p>
            <a href="{config.relative_path}/user/{kernelPosts.user.userslug}">
              <!-- IF kernelPosts.user.picture -->
              <img class="user-img" title="{kernelPosts.user.username}" alt="{kernelPosts.user.username}" src="{kernelPosts.user.picture}" title="{kernelPosts.user.username}"/>
              <!-- ELSE -->
              <span class="user-icon user-img" title="{kernelPosts.user.username}" style="background-color: {kernelPosts.user.icon:bgColor};">{kernelPosts.user.icon:text}</span>
              <!-- ENDIF kernelPosts.user.picture -->
            </a>
            <a class="permalink" href="{config.relative_path}/topic/{kernelPosts.topic.slug}<!-- IF kernelPosts.index -->/{kernelPosts.index}<!-- ENDIF kernelPosts.index -->">
              <small class="timeago" title="{kernelPosts.timestampISO}"></small>
            </a>
            </p>
            <div class="post-content">
              {kernelPosts.content}
            </div>
          </div>
        </div>
      </li>
      <!-- END kernelPosts -->
    </ul>
    <!-- ENDIF kernelPosts.length -->
    <p>

    <div class="popular-tags">
    <!-- BEGIN tags -->
    	<span class="inline-block">
    	<a href="{relative_path}/tags/{tags.value}"><span class="tag-item tag-{tags.value}" style="<!-- IF tags.color -->color: {tags.color};<!-- ENDIF tags.color --><!-- IF tags.bgColor -->background-color: {tags.bgColor};<!-- ENDIF tags.bgColor -->">{tags.value}</span><span class="tag-topic-count">{tags.score}</span></a>
    	</span>
    <!-- END tags -->
    </div>

  </div>

</div>


<script>
'use strict';
$(document).ready(function(){$("#kernel-quick-form").submit(function(b){var a=$.trim($("#kernel-quick-input").val());if(!a||a==""){app.alert({title:"消息",message:"没有脚本可以运行",type:"info",timeout:2000});b.preventDefault();return}})});
</script>
