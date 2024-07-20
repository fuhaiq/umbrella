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
 * 
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-07-19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("tb_hotel")
@Schema(name = "TbHotel", description = "")
public class TbHotel extends Model<TbHotel> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "酒店id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Null(message = "主键不能指定", groups = {AddGroup.class})
    @NotNull(message = "主键不能为空", groups = {UpdateGroup.class, DeleteGroup.class})
    private Long id;

    @Schema(description = "酒店名称")
    @TableField("name")
    private String name;

    @Schema(description = "酒店地址")
    @TableField("address")
    private String address;

    @Schema(description = "酒店价格")
    @TableField("price")
    private Integer price;

    @Schema(description = "酒店评分")
    @TableField("score")
    private Integer score;

    @Schema(description = "酒店品牌")
    @TableField("brand")
    private String brand;

    @Schema(description = "所在城市")
    @TableField("city")
    private String city;

    @Schema(description = "酒店星级，1星到5星，1钻到5钻")
    @TableField("star_name")
    private String starName;

    @Schema(description = "商圈")
    @TableField("business")
    private String business;

    @Schema(description = "纬度")
    @TableField("latitude")
    private String latitude;

    @Schema(description = "经度")
    @TableField("longitude")
    private String longitude;

    @Schema(description = "酒店图片")
    @TableField("pic")
    private String pic;

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

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
