package org.umbrella.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.umbrella.api.entity.TUser;
import org.umbrella.common.core.validation.AddGroup;
import org.umbrella.common.core.validation.DeleteGroup;
import org.umbrella.common.core.validation.UpdateGroup;
import org.umbrella.common.util.R;
import org.umbrella.common.util.WebUtils;
import org.umbrella.user.service.ITUserService;

/**
 * <p>
 * 员工表信息 前端控制器
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-12
 */
@RestController
@RequestMapping("/tUser")
@SecurityRequirement(name = "Keycloak")
@RequiredArgsConstructor
@Tag(name = "员工表信息模块")
@Slf4j
public class TUserController {

    private final ITUserService tUserService;

    private final RabbitTemplate rabbitTemplate;

    @GetMapping("page")
    @Operation(summary = "分页查询员工表信息")
    @Parameters({
            @Parameter(name = "page", description = "当前页码,默认1开始", schema = @Schema(type = "integer", defaultValue = "1", minimum = "1")),
            @Parameter(name = "size", description = "每页显示记录数,默认10", schema = @Schema(type = "integer", defaultValue = "10", maximum = "100")),
            @Parameter(description = "排序字段格式: 字段名,(asc|desc). 默认按照创建时间倒排序,支持多字段排序."
                    , name = "sort"
                    , array = @ArraySchema(schema = @Schema(type = "string")))
    })
    public R page(@PageableDefault(page = 1, sort = {"create_time"}, direction = Sort.Direction.DESC) Pageable pageable) {
//        return R.ok(tUserService.page(WebUtils.getPage(pageable)));
        return R.ok(tUserService.listWithOrders(WebUtils.getPage(pageable)));
    }

    @GetMapping("{id}")
    @Operation(summary = "根据id查询员工表信息")
    @SneakyThrows
    public R getById(@PathVariable("id") Long id, @NotNull @AuthenticationPrincipal Jwt jwt) {
//        return R.ok(tUserService.getById(id));
        return R.ok(tUserService.withOrders(id));
    }

    @PostMapping
    @Operation(summary = "新增员工表信息")
    public R save(@Validated(AddGroup.class) @RequestBody TUser tUser) {
        return R.ok(tUserService.save(tUser));
    }

    @PutMapping
    @Operation(summary = "根据id修改员工表信息")
    public R updateById(@Validated(UpdateGroup.class) @RequestBody TUser tUser) {
        return R.ok(tUserService.updateById(tUser));
    }

    @DeleteMapping
    @Operation(summary = "根据id删除员工表信息")
    public R removeById(@Validated(DeleteGroup.class) @RequestBody TUser tUser) {
        return R.ok(tUserService.removeById(tUser));
    }

    @DeleteMapping("{id}")
    @Operation(summary = "根据id删除员工表信息")
    public R removeById(@PathVariable("id") Long id) {
        return R.ok(tUserService.removeById(id));
    }

}
