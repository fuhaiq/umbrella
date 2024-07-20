package org.umbrella.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.umbrella.api.entity.TUser;

/**
 * <p>
 * 员工表信息 Mapper 接口
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-12
 */
@Mapper
public interface TUserMapper extends BaseMapper<TUser> {

}
