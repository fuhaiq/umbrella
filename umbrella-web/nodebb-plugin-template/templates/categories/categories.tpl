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
    <div>
      <!-- Nav tabs -->
      <ul class="nav nav-tabs" role="tablist">
        <li role="presentation" class="active"><a href="#kernel-success" aria-controls="kernel-success" role="tab" data-toggle="tab">成功</a></li>
        <li role="presentation"><a href="#kernel-aborted" aria-controls="kernel-aborted" role="tab" data-toggle="tab">超时</a></li>
        <li role="presentation"><a href="#kernel-syntax" aria-controls="kernel-syntax" role="tab" data-toggle="tab">语法错误</a></li>
      </ul>
      <!-- Tab panes -->
      <div class="tab-content">
        <div role="tabpanel" class="tab-pane active" id="kernel-success">
          <!-- IF success.length -->
          <ul class="categories" itemscope="" itemtype="http://www.schema.org/ItemList">
            <!-- BEGIN success -->
            <li component="post" data-pid="{success.pid}" class="row clearfix">
              <div class="card" style="border-color: {success.category.bgColor};">
          			<div component="post">
              		<p>
                  <a href="{config.relative_path}/user/{success.user.userslug}">
                    <!-- IF success.user.picture -->
                    <img class="user-img" title="{success.user.username}" alt="{success.user.username}" src="{success.user.picture}" title="{success.user.username}"/>
                    <!-- ELSE -->
                    <span class="user-icon user-img" title="{success.user.username}" style="background-color: {success.user.icon:bgColor};">{success.user.icon:text}</span>
                    <!-- ENDIF success.user.picture -->
                  </a>
                  <a class="permalink" href="{config.relative_path}/topic/{success.topic.slug}<!-- IF success.index -->/{success.index}<!-- ENDIF success.index -->">
            				<small class="timeago" title="{success.timestampISO}"></small>
            			</a>
              		</p>
                  <div class="post-content">
                    {success.content}
                  </div>
              	</div>
              </div>
            </li>
            <!-- END success -->
          </ul>
          <!-- ENDIF success.length -->
        </div>
        <div role="tabpanel" class="tab-pane" id="kernel-aborted">
          <!-- IF aborted.length -->
          <ul class="categories" itemscope="" itemtype="http://www.schema.org/ItemList">
            <!-- BEGIN aborted -->
            <li component="post" data-pid="{success.pid}" class="row clearfix">
              <div class="card" style="border-color: {aborted.category.bgColor};">
                <div component="post">
                  <p>
                  <a href="{config.relative_path}/user/{aborted.user.userslug}">
                    <!-- IF aborted.user.picture -->
                    <img class="user-img" title="{aborted.user.username}" alt="{aborted.user.username}" src="{aborted.user.picture}" title="{aborted.user.username}"/>
                    <!-- ELSE -->
                    <span class="user-icon user-img" title="{aborted.user.username}" style="background-color: {aborted.user.icon:bgColor};">{aborted.user.icon:text}</span>
                    <!-- ENDIF aborted.user.picture -->
                  </a>
                  <a class="permalink" href="{config.relative_path}/topic/{aborted.topic.slug}<!-- IF aborted.index -->/{aborted.index}<!-- ENDIF aborted.index -->">
                    <small class="timeago" title="{aborted.timestampISO}"></small>
                  </a>
                  </p>
                  <div class="post-content">
                    {aborted.content}
                  </div>
                </div>
              </div>
            </li>
            <!-- END aborted -->
          </ul>
          <!-- ENDIF aborted.length -->
        </div>
        <div role="tabpanel" class="tab-pane" id="kernel-syntax">
          <!-- IF syntax.length -->
          <ul class="categories" itemscope="" itemtype="http://www.schema.org/ItemList">
            <!-- BEGIN syntax -->
            <li component="post" data-pid="{success.pid}" class="row clearfix">
              <div class="card" style="border-color: {syntax.category.bgColor};">
                <div component="post">
                  <p>
                  <a href="{config.relative_path}/user/{syntax.user.userslug}">
                    <!-- IF syntax.user.picture -->
                    <img class="user-img" title="{syntax.user.username}" alt="{syntax.user.username}" src="{syntax.user.picture}" title="{syntax.user.username}"/>
                    <!-- ELSE -->
                    <span class="user-icon user-img" title="{syntax.user.username}" style="background-color: {syntax.user.icon:bgColor};">{syntax.user.icon:text}</span>
                    <!-- ENDIF syntax.user.picture -->
                  </a>
                  <a class="permalink" href="{config.relative_path}/topic/{syntax.topic.slug}<!-- IF syntax.index -->/{syntax.index}<!-- ENDIF syntax.index -->">
                    <small class="timeago" title="{syntax.timestampISO}"></small>
                  </a>
                  </p>
                  <div class="post-content">
                    {syntax.content}
                  </div>
                </div>
              </div>
            </li>
            <!-- END syntax -->
          </ul>
          <!-- ENDIF syntax.length -->
        </div>
      </div>
    </div>
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
