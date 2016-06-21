<div class="row">

  <!-- IF similar.length -->
  <div class="topic col-lg-10">
    <!-- ELSE -->
    <div class="topic col-lg-12">
      <!-- ENDIF similar.length -->


      <!-- IMPORT partials/breadcrumbs.tpl -->

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
        <li component="post" class="<!-- IF posts.deleted -->deleted<!-- ENDIF posts.deleted -->" <!-- IMPORT partials/data/topic.tpl -->>
          <a component="post/anchor" data-index="{posts.index}" name="{posts.index}"></a>

          <meta itemprop="datePublished" content="{posts.timestampISO}">
          <meta itemprop="dateModified" content="{posts.editedISO}">

          <!-- IMPORT partials/topic/post.tpl -->
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
    <div class="topic col-lg-2">
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



  </div>
  <!-- IF !config.usePagination -->
  <noscript>
    <!-- IMPORT partials/paginator.tpl -->
  </noscript>
  <!-- ENDIF !config.usePagination -->
