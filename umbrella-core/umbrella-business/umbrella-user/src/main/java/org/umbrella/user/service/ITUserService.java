package org.umbrella.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.umbrella.api.dto.UserDTO;
import org.umbrella.api.entity.TUser;

/**
 * <p>
 * 员工表信息 服务类
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-12
 *
 */
public interface ITUserService extends IService<TUser> {

    IPage<UserDTO> listWithOrders(IPage<TUser> page);

    UserDTO withOrders(Long id);

}
