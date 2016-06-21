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
    <ul class="categories">
	<p>[[template:recent_topic]]</p>
</ul>

<div class="row recent-cards" itemscope itemtype="http://www.schema.org/ItemList">
	<!-- BEGIN topics -->
	<div component="categories/category" class="<!-- IF topics.category.class -->{topics.category.class}<!-- ELSE -->col-md-3 col-sm-6 col-xs-12<!-- ENDIF topics.category.class --> category-item" data-cid="{topics.category.cid}" data-numRecentReplies="{topics.category.numRecentReplies}" style="text-shadow:{recentCards.textShadow};">
		<meta itemprop="name" content="{topics.category.name}">

		<div class="category-icon">
			<div class="bg" style="opacity:{recentCards.opacity};<!-- IF topics.category.backgroundImage -->background-image: url({topics.category.backgroundImage});<!-- ELSE --><!-- IF topics.category.bgColor -->background-color: {topics.category.bgColor};<!-- ENDIF topics.category.bgColor --><!-- ENDIF topics.category.backgroundImage -->"></div>
			<a style="color: {topics.category.color};" href="{config.relative_path}/topic/{topics.slug}" itemprop="url">
				<div
					id="category-{topics.category.cid}" class="category-header category-header-image-{topics.category.imageClass}"
					style="color: {topics.category.color};"
				>
					<!-- IF topics.category.icon -->
					<div><i class="fa {topics.category.icon} fa-4x hidden-xs"></i></div>
					<!-- ENDIF topics.category.icon -->
				</div>
			</a>

			<div class="category-box">
				<div class="category-info" style="color: {topics.category.color};">
					<a href="{config.relative_path}/topic/{topics.slug}" itemprop="url" style="color: {topics.category.color};">
						<h4><!-- IF topics.category.icon --><i class="fa {topics.category.icon} visible-xs-inline"></i> <!-- ENDIF topics.category.icon -->{topics.title}</h4>
						<div class="description" itemprop="description"><strong>{topics.category.name}</strong> <span class="timeago" title="{topics.teaser.timestampISO}"></span></div>
					</a>
				</div>
			</div>

			<span class="post-count" style="color: {topics.category.color};">{topics.postcount}</span>
		</div>
	</div>
	<!-- END topics -->
</div>
<br />
<!-- IF breadcrumbs.length -->
<ol class="breadcrumb">
	<!-- BEGIN breadcrumbs -->
	<li<!-- IF @last --> component="breadcrumb/current"<!-- ENDIF @last --> itemscope="itemscope" itemtype="http://data-vocabulary.org/Breadcrumb" <!-- IF @last -->class="active"<!-- ENDIF @last -->>
		<!-- IF !@last --><a href="{breadcrumbs.url}" itemprop="url"><!-- ENDIF !@last -->
			<span itemprop="title">
				{breadcrumbs.text}
				<!-- IF @last -->
				<!-- IF !feeds:disableRSS -->
				<!-- IF rssFeedUrl --><a target="_blank" href="{rssFeedUrl}"><i class="fa fa-rss-square"></i></a><!-- ENDIF rssFeedUrl --><!-- ENDIF !feeds:disableRSS -->
				<!-- ENDIF @last -->
			</span>
		<!-- IF !@last --></a><!-- ENDIF !@last -->
	</li>
	<!-- END breadcrumbs -->
</ol>
<!-- ENDIF breadcrumbs.length -->
<h1 class="categories-title">[[pages:categories]]</h1>
<ul class="categories" itemscope itemtype="http://www.schema.org/ItemList">
	<!-- BEGIN categories -->
<li component="categories/category" data-cid="{../cid}" data-numRecentReplies="1" class="row clearfix">
	<meta itemprop="name" content="{../name}">

	<div class="col-md-7 col-sm-9 col-xs-12 content">
		<div class="icon pull-left" style="{function.generateCategoryBackground}">
			<i class="fa fa-fw {../icon}"></i>
		</div>

		<h2 class="title">
<!-- IF ../link -->
<a href="{../link}" itemprop="url" target="_blank">
<!-- ELSE -->
<a href="{config.relative_path}/category/{../slug}" itemprop="url">
<!-- ENDIF ../link -->
{../name}
</a><br/>
			<!-- IF ../descriptionParsed -->
			<div class="description">
			{../descriptionParsed}
			</div>
			<!-- ENDIF ../descriptionParsed -->
			{function.generateChildrenCategories}
		</h2>
		<span class="visible-xs pull-right">
			<a class="permalink" href="{../teaser.url}">
				<small class="timeago" title="{../teaser.timestampISO}"></small>
			</a>
		</span>
	</div>

	<!-- IF !../link -->
	<div class="col-md-1 hidden-sm hidden-xs stats">
		<span class="{../unread-class} human-readable-number" title="{../totalTopicCount}">{../totalTopicCount}</span><br />
		<small>[[global:topics]]</small>
	</div>
	<div class="col-md-1 hidden-sm hidden-xs stats">
		<span class="{../unread-class} human-readable-number" title="{../totalPostCount}">{../totalPostCount}</span><br />
		<small>[[global:posts]]</small>
	</div>
	<div class="col-md-3 col-sm-3 teaser hidden-xs">
<div class="card" style="border-color: {../bgColor}">
	<!-- BEGIN posts -->
	<!-- IF @first -->
	<div component="category/posts">
		<p>
			<a href="{config.relative_path}/user/{../user.userslug}">
				<!-- IF ../user.picture -->
				<img class="user-img" title="{../user.username}" alt="{../user.username}" src="{../user.picture}" title="{../user.username}"/>
				<!-- ELSE -->
				<span class="user-icon user-img" title="{../user.username}" style="background-color: {../user.icon:bgColor};">{../user.icon:text}</span>
				<!-- ENDIF ../user.picture -->
			</a>
			<a class="permalink" href="{config.relative_path}/topic/{../topic.slug}<!-- IF ../index -->/{../index}<!-- ENDIF ../index -->">
				<small class="timeago" title="{../timestampISO}"></small>
			</a>
		</p>
		<div class="post-content">
			{../content}
		</div>
	</div>
	<!-- ENDIF @first -->
	<!-- END posts -->

	<!-- IF !../posts.length -->
	<div component="category/posts">
		<div class="post-content">
			[[category:no_new_posts]]
		</div>
	</div>
	<!-- ENDIF !../posts.length -->
</div>
	</div>
	<!-- ENDIF !../link -->
</li>
	<!-- END categories -->
</ul>

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
