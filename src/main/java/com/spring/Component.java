package com.spring;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 标识组件
 *
 * @author 张贝易
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    /**
     * bean的名称
     *
     * @return bean的名称
     */
    String value();
}
