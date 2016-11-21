<!-- IMPORT partials/breadcrumbs.tpl -->
<div widget-area="header"></div>
<div class="row">


	<!-- IF similar.length -->
	<div class="topic col-lg-10 col-sm-10" has-widget-class="topic col-lg-9 col-sm-10" has-widget-target="sidebar">
  <!-- ELSE -->
	<div class="topic col-lg-12 col-sm-12" has-widget-class="topic col-lg-9 col-sm-12" has-widget-target="sidebar">
  <!-- ENDIF similar.length -->

		<h1 component="post/header" class="hidden-xs" itemprop="name">

			<i class="pull-left fa fa-thumb-tack <!-- IF !pinned -->hidden<!-- ENDIF !pinned -->" title="[[topic:pinned]]"></i>
			<i class="pull-left fa fa-lock <!-- IF !locked -->hidden<!-- ENDIF !locked -->" title="[[topic:locked]]"></i>
			<i class="pull-left fa fa-arrow-circle-right <!-- IF !oldCid -->hidden<!-- ENDIF !oldCid -->" title="[[topic:moved]]"></i>
			<!-- BEGIN icons -->@value<!-- END icons -->

			<span class="topic-title" component="topic/title">{title}</span>
		</h1>

		<div component="topic/deleted/message" class="alert alert-warning<!-- IF !deleted --> hidden<!-- ENDIF !deleted -->">[[topic:deleted_message]]</div>

		<hr class="visible-xs" />

		<ul component="topic" class="posts" data-tid="{tid}" data-cid="{cid}">
			<!-- BEGIN posts -->
				<li component="post" class="<!-- IF posts.deleted -->deleted<!-- ENDIF posts.deleted -->" <!-- IMPORT partials/data/topic.tpl -->>
					<a component="post/anchor" data-index="{posts.index}" name="{posts.index}"></a>

					<meta itemprop="datePublished" content="{posts.timestampISO}">
					<meta itemprop="dateModified" content="{posts.editedISO}">

					<!-- IMPORT partials/topic/post.tpl -->
					<!-- IF !posts.index -->
					<div class="post-bar-placeholder"></div>
					<!-- ENDIF !posts.index -->
				</li>
			<!-- END posts -->
		</ul>

		<div class="post-bar">
			<!-- IMPORT partials/post_bar.tpl -->
		</div>

		<!-- IF config.usePagination -->
		<!-- IMPORT partials/paginator.tpl -->
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
	<div class="topic col-lg-2 col-sm-2" has-widget-class="topic col-lg-2 col-sm-2" has-widget-target="sidebar">
		<h4>相似话题</h4>
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
							<small>{similar.postcount}帖子 | {similar.viewcount}浏览</small>
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



	<div widget-area="sidebar" class="col-lg-3 col-sm-12 hidden"></div>
</div>
<div widget-area="footer"></div>

<!-- IF !config.usePagination -->
<noscript>
	<!-- IMPORT partials/paginator.tpl -->
</noscript>
<!-- ENDIF !config.usePagination -->
