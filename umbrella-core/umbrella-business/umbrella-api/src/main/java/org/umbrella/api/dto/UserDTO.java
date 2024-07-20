package org.umbrella.api.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.umbrella.api.entity.TOrder;
import org.umbrella.api.entity.TUser;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@TableName(value = "users", excludeProperty = {"orderList"})
@Data
public class UserDTO extends TUser {
    private List<TOrder> orderList;
}
