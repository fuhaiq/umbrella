package org.umbrella.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@UtilityClass
public class WebUtils extends org.springframework.web.util.WebUtils {

    public Optional<HttpServletRequest> getRequest() {

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes == null ? Optional.empty() :
                Optional.of(requestAttributes.getRequest());
    }

    public Optional<HttpServletResponse> getResponse() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (requestAttributes == null || requestAttributes.getResponse() == null) ? Optional.empty() :
                Optional.of(requestAttributes.getResponse());
    }

    public static <T> IPage<T> getPage(Pageable pageable) {
        var orderItemList = pageable.getSort().stream()
                .filter(order -> !SqlInjectionUtils.check(order.getProperty()))
                .map(order ->
                        order.getDirection() == Sort.Direction.ASC ?
                                OrderItem.asc(order.getProperty()) : OrderItem.desc(order.getProperty())
                ).toList();
        var page = new Page<T>(pageable.getPageNumber(), pageable.getPageSize());
        page.addOrder(orderItemList);
        return page;
    }

}
