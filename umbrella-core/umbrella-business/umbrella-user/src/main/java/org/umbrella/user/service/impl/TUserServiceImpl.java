package org.umbrella.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.umbrella.api.dto.UserDTO;
import org.umbrella.api.entity.TOrder;
import org.umbrella.api.entity.TUser;
import org.umbrella.common.duckdb.DuckdbSession;
import org.umbrella.user.mapper.TOrderMapper;
import org.umbrella.user.mapper.TUserMapper;
import org.umbrella.user.mapper.duckdb.DuckdbMapper;
import org.umbrella.user.service.ITUserService;

/**
 * <p>
 * 员工表信息 服务实现类
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-12
 */
@Service
@RequiredArgsConstructor
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements ITUserService {

    private final DuckdbSession duckdb;

    private final TOrderMapper orderMapper;

    @Override
    public IPage<UserDTO> listWithOrders(IPage<TUser> page) {
        var userPage = page(page);
        var users = userPage.getRecords();

        var uids = users.stream().map(TUser::getId).toList();
        var orders = orderMapper.selectList(Wrappers.lambdaQuery(TOrder.class).in(TOrder::getUserId, uids));

        try (duckdb) {
            duckdb.start();
            duckdb.define("users").from(TUser.class, users);
            duckdb.define("orders").from(TOrder.class, orders);
            var mapper = duckdb.mapper(DuckdbMapper.class);
            var ret = mapper.listWithOrders();
            return new Page<UserDTO>()
                    .setCurrent(userPage.getCurrent())
                    .setSize(userPage.getSize())
                    .setTotal(userPage.getTotal())
                    .setRecords(ret);
        }
    }

    @Override
    public UserDTO withOrders(Long id) {
        var user = getById(id);
        var orders = orderMapper.selectList(Wrappers.lambdaQuery(TOrder.class).eq(TOrder::getUserId, id));
        try (duckdb) {
            duckdb.start();
            duckdb.define("users").from(TUser.class, user);
            duckdb.define("orders").from(TOrder.class, orders);
            var mapper = duckdb.mapper(DuckdbMapper.class);
            return mapper.withOrders();
        }
    }
}
