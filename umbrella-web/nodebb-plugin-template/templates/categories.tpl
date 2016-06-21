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
          <div class="bg" style="opacity:{recentCards.opacity};<!-- IF topics.category.image -->background-image: url({topics.category.image});<!-- ELSE --><!-- IF topics.category.bgColor -->background-color: {topics.category.bgColor};<!-- ENDIF topics.category.bgColor --><!-- ENDIF topics.category.image -->"></div>
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


  <!-- IMPORT partials/breadcrumbs.tpl -->
  <h1 class="categories-title">[[pages:categories]]</h1>
  <ul class="categories" itemscope itemtype="http://www.schema.org/ItemList">
    <!-- BEGIN categories -->
    <!-- IMPORT partials/categories/item.tpl -->
    <!-- END categories -->
  </ul>


</div>

<div class="col-lg-3">
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
