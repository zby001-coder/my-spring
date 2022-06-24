package com.zby;

import com.spring.ZbyApplicationContext;

public class Test {
    public static void main(String[] args) {
        ZbyApplicationContext zbyApplicationContext = new ZbyApplicationContext(AppConfig.class);
        Object userService1 = zbyApplicationContext.getBean("userService");
        Object userService2 = zbyApplicationContext.getBean("userService");
        Object userService3 = zbyApplicationContext.getBean("userService");
        System.out.println(userService1);
        System.out.println(userService2);
        System.out.println(userService3);
    }
}
