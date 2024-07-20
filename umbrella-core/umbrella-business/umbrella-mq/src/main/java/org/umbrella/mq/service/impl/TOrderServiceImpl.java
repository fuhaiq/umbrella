package org.umbrella.mq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.umbrella.api.entity.TOrder;
import org.umbrella.mq.mapper.TOrderMapper;
import org.umbrella.mq.service.ITOrderService;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author fuhaiq@gmail.com
 * @since 2024-06-12
 */
@Service
public class TOrderServiceImpl extends ServiceImpl<TOrderMapper, TOrder> implements ITOrderService {
}
