package org.umbrella.user.mapper.duckdb;

import org.apache.ibatis.annotations.Mapper;
import org.umbrella.api.dto.UserDTO;

import java.util.List;

@Mapper
public interface DuckdbMapper {
    List<UserDTO> listWithOrders();

    UserDTO withOrders();
}
