package org.umbrella.api.feign;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.umbrella.api.entity.TUser;
import org.umbrella.common.feign.OpenFeignConfiguration;
import org.umbrella.common.util.R;

@FeignClient(value = "user-service", configuration = OpenFeignConfiguration.class)
public interface RemoteUserService {

    /*
    客户发起的远程调用
     */
    @GetMapping("/tUser/{id}")
    R<TUser> getById(@PathVariable("id") Long id);

    /*
    定时任务发起的远程调用
     */
    @GetMapping("/tUser/{id}")
    R<TUser> getById(@PathVariable("id") Long id, @NotNull @RequestHeader(StringPool.YES) String inner);

}