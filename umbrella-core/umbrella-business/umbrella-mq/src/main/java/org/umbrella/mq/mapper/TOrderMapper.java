package org.umbrella.mq.mapper;

import org.umbrella.api.entity.TOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 订单表 Mapper 接口
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-12
 */
@Mapper
public interface TOrderMapper extends BaseMapper<TOrder> {

}
