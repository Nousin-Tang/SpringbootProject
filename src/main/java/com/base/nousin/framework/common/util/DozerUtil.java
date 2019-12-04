package com.base.nousin.framework.common.util;

import com.base.nousin.framework.config.SpringContextHolder;
import com.github.dozermapper.core.Mapper;

/**
 * 类与类之间转化工具类
 *
 * @author tangwc
 * @since 2019-12-2
 */
public class DozerUtil {
    public static Mapper mapper = SpringContextHolder.getBean(Mapper.class);
    public static <T, P> T map(P p, Class<T> t){
        return mapper.map(p,t);
    }
}
