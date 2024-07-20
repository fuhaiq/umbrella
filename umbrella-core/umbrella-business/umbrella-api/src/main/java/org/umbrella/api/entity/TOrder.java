package org.umbrella.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.umbrella.common.core.validation.AddGroup;
import org.umbrella.common.core.validation.DeleteGroup;
import org.umbrella.common.core.validation.UpdateGroup;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-12
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_order")
@Schema(name = "TOrder", description = "订单表")
@AllArgsConstructor
public class TOrder extends Model<TOrder> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Null(message = "主键不能指定", groups = {AddGroup.class})
    @NotNull(message = "主键不能为空", groups = {UpdateGroup.class, DeleteGroup.class})
    private Long id;

    @Schema(description = "版本号")
    @TableField(value = "version", fill = FieldFill.INSERT)
    @Version
    @Null(message = "不能指定版本号", groups = {AddGroup.class})
    @NotNull(message = "版本号不能为空", groups = {UpdateGroup.class, DeleteGroup.class})
    private Long version;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Null(message = "不能指定创建时间", groups = {AddGroup.class, UpdateGroup.class})
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Null(message = "不能指定更新时间", groups = {AddGroup.class, UpdateGroup.class})
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Null(message = "不能指定创建人", groups = {AddGroup.class, UpdateGroup.class})
    private String createBy;

    @Schema(description = "更新人")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Null(message = "不能指定更新人", groups = {AddGroup.class, UpdateGroup.class})
    private String updateBy;

    @Schema(description = "用户ID")
    @TableField("user_id")
    @NotNull(message = "用户ID不能为空", groups = {AddGroup.class})
    private Long userId;

    @Schema(description = "商品名称")
    @TableField("product_name")
    @NotNull(message = "商品名称不能为空", groups = {AddGroup.class})
    private String productName;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
