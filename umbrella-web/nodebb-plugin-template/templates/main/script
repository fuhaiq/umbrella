$(document).ready(function(){
  $("#kernel-quick-form").submit(function(event){
    var content = $.trim($("#kernel-quick-input").val());
    if(!content || content == '') {
        app.alert({
            title: '消息',
            message: '没有脚本可以运行',
            type: 'info',
            timeout: 2000
        });
        event.preventDefault();
        return;
    }
  })
})
