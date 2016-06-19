<div class="row">
	<!-- IF similar.length -->
	<div class="topic col-lg-10">
	<!-- ELSE -->
	<div class="topic col-lg-12">
	<!-- ENDIF similar.length -->
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

	<h1 component="post/header" class="hidden-xs" itemprop="name">

		<i class="pull-left fa fa-thumb-tack <!-- IF !pinned -->hidden<!-- ENDIF !pinned -->"></i> <i class="pull-left fa fa-lock <!-- IF !locked -->hidden<!-- ENDIF !locked -->"></i> <span class="topic-title" component="topic/title">{title}</span>

		<span class="browsing-users hidden hidden-xs hidden-sm pull-right">
			<span>[[category:browsing]]</span>
			<div component="topic/browsing/list" class="thread_active_users active-users inline-block"></div>
			<small class="hidden">
				<i class="fa fa-users"></i> <span component="topic/browsing/count" class="user-count"></span>
			</small>
		</span>
	</h1>

	<div component="topic/deleted/message" class="alert alert-warning<!-- IF !deleted --> hidden<!-- ENDIF !deleted -->">[[topic:deleted_message]]</div>

	<hr class="visible-xs" />

	<ul component="topic" class="posts" data-tid="{tid}">
		<!-- BEGIN posts -->
			<li component="post" class="<!-- IF posts.deleted -->deleted<!-- ENDIF posts.deleted -->"data-index="{posts.index}" data-pid="{posts.pid}" data-uid="{posts.uid}" data-username="{posts.user.username}" data-userslug="{posts.user.userslug}" itemscope itemtype="http://schema.org/Comment">
				<a component="post/anchor" data-index="{posts.index}" name="{posts.index}"></a>

				<meta itemprop="datePublished" content="{posts.timestampISO}">
				<meta itemprop="dateModified" content="{posts.editedISO}">

<div class="clearfix">
	<div class="icon pull-left">
		<a href="<!-- IF posts.user.userslug -->{config.relative_path}/user/{posts.user.userslug}<!-- ELSE -->#<!-- ENDIF posts.user.userslug -->">
			<!-- IF posts.user.picture -->
			<img component="user/picture" data-uid="{posts.user.uid}" src="{posts.user.picture}" align="left" itemprop="image" />
			<!-- ELSE -->
			<div component="user/picture" data-uid="{posts.user.uid}" class="user-icon" style="background-color: {posts.user.icon:bgColor};">{posts.user.icon:text}</div>
			<!-- ENDIF posts.user.picture -->
			<i component="user/status" class="fa fa-circle status {posts.user.status}" title="[[global:{posts.user.status}]]"></i>

		</a>
	</div>

	<small class="pull-left">
		<strong>
			<a href="<!-- IF posts.user.userslug -->{config.relative_path}/user/{posts.user.userslug}<!-- ELSE -->#<!-- ENDIF posts.user.userslug -->" itemprop="author" data-username="{posts.user.username}" data-uid="{posts.user.uid}">{posts.user.username}</a>
		</strong>

<!-- IF posts.user.selectedGroup.slug -->
<a href="{config.relative_path}/groups/{posts.user.selectedGroup.slug}"><small class="label group-label inline-block" style="background-color: {posts.user.selectedGroup.labelColor};"><!-- IF posts.user.selectedGroup.icon --><i class="fa {posts.user.selectedGroup.icon}"></i> <!-- ENDIF posts.user.selectedGroup.icon -->{posts.user.selectedGroup.userTitle}</small></a>
<!-- ENDIF posts.user.selectedGroup.slug -->

		<!-- IF posts.user.banned -->
		<span class="label label-danger">[[user:banned]]</span>
		<!-- ENDIF posts.user.banned -->

		<div class="visible-xs-inline-block visible-sm-inline-block visible-md-inline-block visible-lg-inline-block">
			<a class="permalink" href="{config.relative_path}/topic/{slug}/{function.getBookmarkFromIndex}"><span class="timeago" title="{posts.timestampISO}"></span></a>

			<i class="fa fa-pencil-square pointer edit-icon <!-- IF !posts.editor.username -->hidden<!-- ENDIF !posts.editor.username -->"></i>

			<small data-editor="{posts.editor.userslug}" component="post/editor" class="hidden">[[global:last_edited_by, {posts.editor.username}]] <span class="timeago" title="{posts.editedISO}"></span></small>

			<!-- IF posts.toPid -->
			<button component="post/parent" class="btn btn-xs btn-default hidden-xs" data-topid="{posts.toPid}"><i class="fa fa-reply"></i> @<!-- IF posts.parent.username -->{posts.parent.username}<!-- ELSE -->[[global:guest]]<!-- ENDIF posts.parent.username --></button>
			<!-- ENDIF posts.toPid -->

			<span>
				<!-- IF posts.user.custom_profile_info.length -->
				&#124;
				<!-- BEGIN custom_profile_info -->
				{posts.user.custom_profile_info.content}
				<!-- END custom_profile_info -->
				<!-- ENDIF posts.user.custom_profile_info.length -->
			</span>
		</div>
		<span class="bookmarked"><i class="fa fa-bookmark-o"></i></span>

	</small>
</div>

<br />

<div class="content" component="post/content" itemprop="text">
	{posts.content}
</div>

<div class="clearfix">
	<!-- IF posts.user.signature -->
	<div component="post/signature" data-uid="{posts.user.uid}" class="post-signature">{posts.user.signature}</div>
	<!-- ENDIF posts.user.signature -->

	<small class="pull-right">
		<span class="post-tools">
			<a component="post/reply" href="#" class="no-select <!-- IF !privileges.topics:reply -->hidden<!-- ENDIF !privileges.topics:reply -->">[[topic:reply]]</a>
			<a component="post/quote" href="#" class="no-select <!-- IF !privileges.topics:reply -->hidden<!-- ENDIF !privileges.topics:reply -->">[[topic:quote]]</a>
		</span>

		<!-- IF !reputation:disabled -->
		<span class="votes">
			<a component="post/upvote" href="#" class="<!-- IF posts.upvoted -->upvoted<!-- ENDIF posts.upvoted -->">
				<i class="fa fa-chevron-up"></i>
			</a>

			<span component="post/vote-count" data-votes="{posts.votes}">{posts.votes}</span>

			<!-- IF !downvote:disabled -->
			<a component="post/downvote" href="#" class="<!-- IF posts.downvoted -->downvoted<!-- ENDIF posts.downvoted -->">
				<i class="fa fa-chevron-down"></i>
			</a>
			<!-- ENDIF !downvote:disabled -->
		</span>
		<!-- ENDIF !reputation:disabled -->

<span component="post/tools" class="dropdown moderator-tools <!-- IF !posts.display_post_menu -->hidden<!-- ENDIF !posts.display_post_menu -->">
	<a href="#" data-toggle="dropdown"><i class="fa fa-fw fa-ellipsis-v"></i></a>
	<ul class="dropdown-menu dropdown-menu-right" role="menu"></ul>
</span>

	</small>
</div>

<hr />

			</li>
		<!-- END posts -->
	</ul>

	<div class="post-bar">
<div class="clearfix">
	<div class="tags pull-left">
		<!-- BEGIN tags -->
		<a href="{config.relative_path}/tags/{tags.value}">
		<span class="tag-item" data-tag="{tags.value}" style="<!-- IF tags.color -->color: {tags.color};<!-- ENDIF tags.color --><!-- IF tags.bgColor -->background-color: {tags.bgColor};<!-- ENDIF tags.bgColor -->">{tags.value}</span>
		<span class="tag-topic-count human-readable-number" title="{tags.score}">{tags.score}</span></a>
		<!-- END tags -->
	</div>

	<div class="topic-main-buttons pull-right">
		<span class="loading-indicator btn pull-left hidden" done="0">
			<span class="hidden-xs">[[topic:loading_more_posts]]</span> <i class="fa fa-refresh fa-spin"></i>
		</span>

<div class="stats hidden-xs">
	<span component="topic/post-count" class="human-readable-number" title="{postcount}">{postcount}</span><br />
	<small>[[global:posts]]</small>
</div>
<div class="stats hidden-xs">
	<span class="human-readable-number" title="{viewcount}">{viewcount}</span><br />
	<small>[[global:views]]</small>
</div>

<div class="btn-group action-bar <!-- IF !privileges.topics:reply -->hidden<!-- ENDIF !privileges.topics:reply -->">
	<button class="btn btn-primary" component="topic/reply">[[topic:reply]]</button>
	<button type="button" class="btn btn-info dropdown-toggle" data-toggle="dropdown">
		<span class="caret"></span>
	</button>
	<ul class="dropdown-menu pull-right" role="menu">
		<li><a href="#" component="topic/reply-as-topic">[[topic:reply-as-topic]]</a></li>
	</ul>
</div>

<!-- IF loggedIn -->
<!-- IF !privileges.topics:reply -->
<!-- IF locked -->
<a component="topic/reply/locked" class="btn btn-primary" disabled><i class="fa fa-lock"></i> [[topic:locked]]</a>
<!-- ENDIF locked -->
<!-- ENDIF !privileges.topics:reply -->

<!-- IF !locked -->
<a component="topic/reply/locked" class="btn btn-primary hidden" disabled><i class="fa fa-lock"></i> [[topic:locked]]</a>
<!-- ENDIF !locked -->

<!-- ELSE -->

<!-- IF !privileges.topics:reply -->
<a href="{config.relative_path}/login" class="btn btn-primary">[[topic:guest-login-reply]]</a>
<!-- ENDIF !privileges.topics:reply -->
<!-- ENDIF loggedIn -->

<!-- IF config.loggedIn -->
<button component="topic/follow" class="btn btn-info <!-- IF isFollowing -->hidden<!-- ENDIF isFollowing -->">
	<span class="visible-sm-inline visible-md-inline visible-lg-inline">[[topic:watch]]</span> <i class="fa fa-eye"></i>
</button>

<button component="topic/unfollow" class="btn btn-warning <!-- IF !isFollowing -->hidden<!-- ENDIF !isFollowing -->">
	<span class="visible-sm-inline visible-md-inline visible-lg-inline">[[topic:unwatch]]</span> <i class="fa fa-eye-slash"></i>
</button>
<!-- ENDIF config.loggedIn -->

		<!-- IF loggedIn -->
		<button component="topic/mark-unread" class="btn btn-default">
			<span class="visible-sm-inline visible-md-inline visible-lg-inline">[[topic:mark_unread]]</span> <i class="fa fa-inbox"></i>
		</button>
		<!-- ENDIF loggedIn -->

<!-- IF config.loggedIn -->
<div class="btn-group" component="thread/sort">
	<button class="btn btn-default dropdown-toggle" data-toggle="dropdown" type="button"><span class="visible-sm-inline visible-md-inline visible-lg-inline">[[topic:sort_by]]</span>
	<span class="visible-xs-inline"><i class="fa fa-fw fa-sort"></i></span> <span class="caret"></span></button>
	<ul class="dropdown-menu dropdown-menu-right">
		<li><a href="#" class="oldest_to_newest" data-sort="oldest_to_newest"><i class="fa fa-fw"></i> [[topic:oldest_to_newest]]</a></li>
		<li><a href="#" class="newest_to_oldest" data-sort="newest_to_oldest"><i class="fa fa-fw"></i> [[topic:newest_to_oldest]]</a></li>
		<li><a href="#" class="most_votes" data-sort="most_votes"><i class="fa fa-fw"></i> [[topic:most_votes]]</a></li>
	</ul>
</div>
<!-- ENDIF config.loggedIn -->

<!-- IF privileges.view_thread_tools -->
<div class="btn-group thread-tools">
	<button class="btn btn-default dropdown-toggle" data-toggle="dropdown" type="button">
		<span class="visible-sm-inline visible-md-inline visible-lg-inline">[[topic:thread_tools.title]]</span>
		<span class="visible-xs-inline"><i class="fa fa-fw fa-gear"></i></span>
		<span class="caret"></span>
	</button>
	<ul class="dropdown-menu dropdown-menu-right"></ul>
</div>
<!-- ENDIF privileges.view_thread_tools -->
	</div>
</div>
<hr/>
	</div>

	<!-- IF config.usePagination -->
<div component="pagination" class="text-center pagination-container<!-- IF !pagination.pages.length --> hidden<!-- ENDIF !pagination.pages.length -->">
	<ul class="pagination">
		<li class="previous pull-left<!-- IF !pagination.prev.active --> disabled<!-- ENDIF !pagination.prev.active -->">
			<a href="?{pagination.prev.qs}" data-page="{pagination.prev.page}"><i class="fa fa-chevron-left"></i> </a>
		</li>

		<!-- BEGIN pages -->
			<!-- IF pagination.pages.separator -->
			<li component="pagination/select-page" class="page select-page">
				<a href="#"><i class="fa fa-ellipsis-h"></i></a>
			</li>
			<!-- ELSE -->
			<li class="page<!-- IF pagination.pages.active --> active<!-- ELSE --> hidden-xs<!-- ENDIF pagination.pages.active -->" >
				<a href="?{pagination.pages.qs}" data-page="{pagination.pages.page}">{pagination.pages.page}</a>
			</li>
			<!-- ENDIF pagination.pages.separator -->
		<!-- END pages -->

		<li class="next pull-right<!-- IF !pagination.next.active --> disabled<!-- ENDIF !pagination.next.active -->">
			<a href="?{pagination.next.qs}" data-page="{pagination.next.page}"> <i class="fa fa-chevron-right"></i></a>
		</li>
	</ul>
</div>
	<!-- ENDIF config.usePagination -->

	<div class="visible-xs visible-sm pagination-block text-center">
		<div class="progress-bar"></div>
		<div class="wrapper">
			<i class="fa fa-2x fa-angle-double-up pointer fa-fw pagetop"></i>
			<i class="fa fa-2x fa-angle-up pointer fa-fw pageup"></i>
			<span class="pagination-text"></span>
			<i class="fa fa-2x fa-angle-down pointer fa-fw pagedown"></i>
			<i class="fa fa-2x fa-angle-double-down pointer fa-fw pagebottom"></i>
		</div>
	</div>
</div>




<!-- IF similar.length -->
<div class="topic col-lg-2">
  <h4>[[elasticsearch:similar_topics]]</h4>
  <ul class="categories" itemscope="" itemtype="http://www.schema.org/ItemList">
    <!-- BEGIN similar -->
    <li component="category/topic" data-tid={similar.tid} class="row clearfix">
      <div class="card" style="border-color: {similar.category.bgColor}">
        <div component="category/topic">
          <p>
            <a href="{config.relative_path}/user/{similar.user.userslug}">
              <!-- IF similar.user.picture -->
              <img title="{similar.user.username}" class="user-img" src="{similar.user.picture}" />
              <!-- ELSE -->
              <span title="{similar.user.username}" class="user-icon user-img" style="background-color: {similar.user.icon:bgColor};">{similar.user.icon:text}</span>
              <!-- ENDIF similar.user.picture -->
            </a>
            <small>{similar.postcount}[[global:posts]] | {similar.viewcount}[[global:views]]</small>
          </p>
          <div class="post-content">
            <a href="{config.relative_path}/topic/{similar.slug}" itemprop="url">{similar.title}</a>
          </div>
        </div>
      </div>
    </li>
    <!-- END similar -->
  </ul>
</div>
<!-- ENDIF similar.length -->



</div>
<!-- IF !config.usePagination -->
<noscript>
<div component="pagination" class="text-center pagination-container<!-- IF !pagination.pages.length --> hidden<!-- ENDIF !pagination.pages.length -->">
	<ul class="pagination">
		<li class="previous pull-left<!-- IF !pagination.prev.active --> disabled<!-- ENDIF !pagination.prev.active -->">
			<a href="?{pagination.prev.qs}" data-page="{pagination.prev.page}"><i class="fa fa-chevron-left"></i> </a>
		</li>

		<!-- BEGIN pages -->
			<!-- IF pagination.pages.separator -->
			<li component="pagination/select-page" class="page select-page">
				<a href="#"><i class="fa fa-ellipsis-h"></i></a>
			</li>
			<!-- ELSE -->
			<li class="page<!-- IF pagination.pages.active --> active<!-- ELSE --> hidden-xs<!-- ENDIF pagination.pages.active -->" >
				<a href="?{pagination.pages.qs}" data-page="{pagination.pages.page}">{pagination.pages.page}</a>
			</li>
			<!-- ENDIF pagination.pages.separator -->
		<!-- END pages -->

		<li class="next pull-right<!-- IF !pagination.next.active --> disabled<!-- ENDIF !pagination.next.active -->">
			<a href="?{pagination.next.qs}" data-page="{pagination.next.page}"> <i class="fa fa-chevron-right"></i></a>
		</li>
	</ul>
</div>
</noscript>
<!-- ENDIF !config.usePagination -->
