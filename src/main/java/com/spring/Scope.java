package com.spring;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 单例还是原型模式
 *
 * @author 张贝易
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {
    String value() default "";
}
