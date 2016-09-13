<div class="row">
	<div class="col-lg-9">
		<div class="recent">

			<div class="clearfix">
				<div class="pull-left">
					<!-- IMPORT partials/breadcrumbs.tpl -->
				</div>
			</div>
			<p>
				<a type="button" class="btn btn-info btn-xs" href="/morse">摩尔斯电码</a>
			</p>
			<form action="/kernel" method="get" id="kernel-quick-form">
				<div class="input-group has-success input-group-sm">
					<span class="input-group-addon"><input type="checkbox" name="d"><img src="/3d-16.png"></span>
					<input type="text" name="q" class="form-control" placeholder="执行" id="kernel-quick-input">
					<span class="input-group-btn">
						<button class="btn btn-success" type="submit"><i class="fa fa-play" aria-hidden="true"></i></button>
					</span>

				</div>
			</form>

			<hr class="hidden-xs"/>

			<div class="category">
				<!-- IF !topics.length -->
				<div class="alert alert-warning" id="category-no-topics">[[recent:no_recent_topics]]</div>
				<!-- ENDIF !topics.length -->

				<a href="{config.relative_path}/recent">
					<div class="alert alert-warning hide" id="new-topics-alert"></div>
				</a>

				<!-- IMPORT partials/topics_list.tpl -->

				<!-- IF config.usePagination -->
				<!-- IMPORT partials/paginator.tpl -->
				<!-- ENDIF config.usePagination -->
			</div>
		</div>
	</div>
	<div class="col-lg-3">

		<div class="alert alert-warning" role="alert">
			分享, 计算你的Mathematica脚本
			<p>
			<p>
			<!-- IF loggedIn -->
			<button id="new_topic" class="btn btn-primary btn-block">[[category:new_topic_button]]</button>
			<!-- ELSE -->
			<a href="{config.relative_path}/login" class="btn btn-primary btn-block">[[category:guest-login-post]]</a>
			<!-- ENDIF loggedIn -->
			<hr style="margin-bottom: 13px;margin-top: 10px;">
			<i class="fa fa-bullhorn" aria-hidden="true"></i> <a href="/topic/14" class="alert-link" style="font-weight: normal;">Chrome脚本计算插件发布了</a>
		</div>

		<!-- IF kernelPosts.length -->
		<ul class="categories">
			<p>[[template:recent_kernel]]</p>
		</ul>
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
						<!-- IF kernelPosts.finished -->
						<span class="label label-success"><i class="fa fa-check"></i> 计算成功</span>
						<!-- ENDIF kernelPosts.finished -->
						<!-- IF kernelPosts.error -->
						<span class="label label-danger"><i class="fa fa-remove"></i> 语法错误</span>
						<!-- ENDIF kernelPosts.error -->
						<!-- IF kernelPosts.aborted -->
						<span class="label label-warning"><i class="fa fa-exclamation"></i> 计算超时</span>
						<!-- ENDIF kernelPosts.aborted -->
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
$(document).ready(function(){$("#kernel-quick-form").submit(function(b){var a=$.trim($("#kernel-quick-input").val());if(!a||a==""){app.alert({title:"消息",message:"没有脚本可以运行",type:"info",timeout:2000});b.preventDefault();return}})});
</script>
