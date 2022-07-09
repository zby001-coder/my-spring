package com.zby;

import com.spring.ZbyApplicationContext;
import com.zby.service.UserService;

public class Test {
    public static void main(String[] args) {
        ZbyApplicationContext zbyApplicationContext = new ZbyApplicationContext(AppConfig.class);
        UserService userService1 = (UserService) zbyApplicationContext.getBean("userService");
        userService1.test();
    }
}
