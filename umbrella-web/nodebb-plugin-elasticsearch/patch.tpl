#################/node_modules/nodebb-theme-persona/templates/topic.tpl##############

<!-- IF similar.length -->
<div class="topic col-lg-10">
<!-- ELSE -->
<div class="topic col-lg-12">
<!-- ENDIF similar.length -->
---------------------------------

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
