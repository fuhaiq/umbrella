package org.umbrella.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
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
 * 员工表信息
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-05
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_user")
@Schema(name = "TUser", description = "员工表信息")
public class TUser extends Model<TUser> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Null(message = "主键不能指定", groups = {AddGroup.class})
    @NotNull(message = "主键不能为空", groups = {UpdateGroup.class, DeleteGroup.class})
    private Long id;

    @Schema(description = "姓名")
    @TableField("name")
    private String name;

    @Schema(description = "年龄")
    @TableField("age")
    private Integer age;

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

    @Schema(description = "创建者")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Null(message = "不能指定创建人", groups = {AddGroup.class, UpdateGroup.class})
    private String createBy;

    @Schema(description = "更新者")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Null(message = "不能指定更新人", groups = {AddGroup.class, UpdateGroup.class})
    private String updateBy;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
