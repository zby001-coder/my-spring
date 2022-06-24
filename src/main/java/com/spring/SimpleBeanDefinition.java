package com.spring;

import java.lang.reflect.Type;

public class SimpleBeanDefinition implements BeanDefinition {
    private static final String SCOPE_DEFAULT = "";
    private String scope = SCOPE_DEFAULT;
    private Type type;

    public String getScope() {
        return scope;
    }

    public SimpleBeanDefinition setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public SimpleBeanDefinition setType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean isSingleton() {
        return (SCOPE_SINGLETON.equals(this.scope) || SCOPE_DEFAULT.equals(this.scope));
    }

    @Override
    public boolean isPrototype() {
        return SCOPE_PROTOTYPE.equals(this.scope);
    }

    @Override
    public Type getType() {
        return this.type;
    }
}
