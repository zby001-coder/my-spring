package com.spring;

import java.lang.reflect.Type;

public interface BeanDefinition {
    String SCOPE_SINGLETON = "singleton";
    String SCOPE_PROTOTYPE = "prototype";

    boolean isSingleton();

    boolean isPrototype();

    Type getType();
}
