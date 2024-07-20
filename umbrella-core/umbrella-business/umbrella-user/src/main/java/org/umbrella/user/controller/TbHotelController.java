package org.umbrella.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.umbrella.user.service.ITbHotelService;
import org.umbrella.common.util.R;
import org.umbrella.common.util.WebUtils;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.umbrella.api.entity.TbHotel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.umbrella.common.core.validation.AddGroup;
import org.umbrella.common.core.validation.UpdateGroup;
import org.umbrella.common.core.validation.DeleteGroup;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-07-19
 */
@RestController
@RequestMapping("/user/tbHotel")
@AllArgsConstructor
@SecurityRequirement(name = "Keycloak")
@Tag(name = "模块")
public class TbHotelController {

    private final ITbHotelService tbHotelService;

    @GetMapping("page")
    @Operation(summary = "分页查询")
    @Parameters({
        @Parameter(name = "page", description = "当前页码,默认1开始", schema = @Schema(type = "integer", defaultValue = "1", minimum = "1")),
        @Parameter(name = "size", description = "每页显示记录数,默认10", schema = @Schema(type = "integer", defaultValue = "10", maximum = "100")),
        @Parameter(description = "排序字段格式: 字段名,(asc|desc). 默认按照创建时间倒排序,支持多字段排序."
                , name = "sort"
                , array = @ArraySchema(schema = @Schema(type = "string")))
    })
    public R page(@PageableDefault(page = 1, sort = {"create_time"}, direction = Sort.Direction.DESC) Pageable pageable){
        return R.ok(tbHotelService.page(WebUtils.getPage(pageable)));
    }

    @GetMapping("{id}")
    @Operation(summary = "根据id查询")
    public R getById(@PathVariable("id") Long id){
        return R.ok(tbHotelService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增")
    public R save(@Validated(AddGroup.class) @RequestBody TbHotel tbHotel){
        return R.ok(tbHotelService.save(tbHotel));
    }

    @PutMapping
    @Operation(summary = "根据id修改")
    public R updateById(@Validated(UpdateGroup.class) @RequestBody TbHotel tbHotel){
        return R.ok(tbHotelService.updateById(tbHotel));
    }

    @DeleteMapping
    @Operation(summary = "根据id删除")
    public R removeById(@Validated(DeleteGroup.class) @RequestBody TbHotel tbHotel){
        return R.ok(tbHotelService.removeById(tbHotel));
    }

    @DeleteMapping("{id}")
    @Operation(summary = "根据id删除")
    public R removeById(@PathVariable("id") Long id){
        return R.ok(tbHotelService.removeById(id));
    }

}
