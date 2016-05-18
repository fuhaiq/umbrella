<!-- Main plugin -->

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
    <!-- Main Original content -->
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
          <ul class="categories" itemscope="" itemtype="http://www.schema.org/ItemList">
            <li component="categories/category" data-cid="1" data-numrecentreplies="1" class="row clearfix">
              <div class="card" style="border-color: #fda34b; margin-top: 0px;height: 45px;">
          			<div component="category/posts">
              		<p>
            			<a href="/user/mike">
    								<span class="user-icon user-img" title="" style="background-color: #9c27b0;" data-original-title="mike">M</span>
            			</a>
            			<a class="permalink" href="/topic/8/欢迎大家来到这个论坛/1">
            				<small class="timeago" title="Tue May 17 2016 16:43:50 GMT+0800 (CST)">大约18小时之前</small>
            			</a>
              		</p>
                  <div class="post-content">
                    <a href="/topic/2/大家好-欢迎来到这个论坛" itemprop="url">大家好,欢迎来到这个论坛</a>
                  </div>
              	</div>
              </div>
            </li>

            <li component="categories/category" data-cid="1" data-numrecentreplies="1" class="row clearfix">
              <div class="card" style="border-color: #fda34b; margin-top: 0px;height: 45px;">
          			<div component="category/posts">
              		<p>
            			<a href="/user/mike">
    								<span class="user-icon user-img" title="" style="background-color: #9c27b0;" data-original-title="mike">M</span>
            			</a>
            			<a class="permalink" href="/topic/8/欢迎大家来到这个论坛/1">
            				<small class="timeago" title="Tue May 17 2016 16:43:50 GMT+0800 (CST)">大约18小时之前</small>
            			</a>
              		</p>
                  <div class="post-content">
                    <a href="/topic/2/大家好-欢迎来到这个论坛" itemprop="url">大家好,欢迎来到这个论坛</a>
                  </div>
              	</div>
              </div>
            </li>

            <li component="categories/category" data-cid="1" data-numrecentreplies="1" class="row clearfix">
              <div class="card" style="border-color: #fda34b; margin-top: 0px;height: 45px;">
          			<div component="category/posts">
              		<p>
            			<a href="/user/mike">
    								<span class="user-icon user-img" title="" style="background-color: #9c27b0;" data-original-title="mike">M</span>
            			</a>
            			<a class="permalink" href="/topic/8/欢迎大家来到这个论坛/1">
            				<small class="timeago" title="Tue May 17 2016 16:43:50 GMT+0800 (CST)">大约18小时之前</small>
            			</a>
              		</p>
                  <div class="post-content">
                    <a href="/topic/2/大家好-欢迎来到这个论坛" itemprop="url">大家好,欢迎来到这个论坛</a>
                  </div>
              	</div>
              </div>
            </li>

          </ul>
        </div>
        <div role="tabpanel" class="tab-pane" id="kernel-aborted">


        </div>
        <div role="tabpanel" class="tab-pane" id="kernel-syntax">


        </div>
      </div>
    </div>
    <p>

    <ul class="categories">
    	<p>热门标签</p>
    </ul>
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
