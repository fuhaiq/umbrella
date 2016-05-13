<!-- Main plugin -->

<div class="row">

  <div class="col-lg-9">
    <form action="/kernel" method="get">
      <div class="input-group input-group-sm">
        <span class="input-group-addon" id="basic-addon1"><i class="fa fa-terminal" aria-hidden="true"></i></span>
        <input type="text" name="q" class="form-control" placeholder="mathematica..." maxlength="100">
        <span class="input-group-btn">
          <button class="btn btn-success" type="submit"><i class="fa fa-play" aria-hidden="true"></i> 执行</button>
        </span>
      </div>
    </form>
    <br>
    <!-- Main Original content -->
  </div>

  <div class="col-lg-3">

    <div class="alert alert-info" role="alert">
      一些说明
    </div>

    <h1 class="categories-title">热门标签</h1>
    <div class="ui large labels">
      <!-- BEGIN tags -->
      <a class="ui label" href="{config.relative_path}/tags/{tags.value}">
        {tags.value}
        <div class="detail">{tags.score}</div>
      </a>
      <!-- END tags -->
    </div>

  </div>

</div>
