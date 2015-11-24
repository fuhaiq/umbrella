/node_modules/nodebb-theme-persona/templates/topic.tpl


<!-- IF similar.length -->
<h4>相似话题</h4>
<table class="table table-striped">
  <thead>
    <tr>
      <th>话题</th>
      <th>版面</th>
      <th>帖子</th>
      <th>浏览</th>
      <th>动态</th>
    </tr>
  </thead>
  <tbody>
    <!-- BEGIN similar -->
    <tr>
      <th><a href="{config.relative_path}/topic/{similar.slug}" itemprop="url">{similar.title}</a></th>
      <td><a href="{config.relative_path}/category/{similar.category.slug}" class="label label-default" style="background-color: {similar.category.bgColor}; color: {similar.category.color};">{similar.category.name}</a></td>
      <td>{similar.postcount}</td>
      <td>{similar.viewcount}</td>
      <td>
      <!-- IF similar.unreplied -->
        <small class="timeago" title="{similar.relativeTime}"></small>
      <!-- ELSE -->
        <small class="timeago" title="{similar.teaser.timestamp}"></small>
      <!-- ENDIF similar.unreplied -->
      </td>
    </tr>
    <!-- END similar -->
  </tbody>
</table>
<!-- ENDIF similar.length -->