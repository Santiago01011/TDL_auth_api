package com.TrashTDL.ServerlessAuth.config;

import com.TrashTDL.ServerlessAuth.ServerlessAuthApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringContextHolder {
    private static ConfigurableApplicationContext context;
    private static final Object lock = new Object();

    public static ConfigurableApplicationContext getContext() {
        if (context == null) {
            synchronized (lock) {
                if (context == null) {
                    context = new SpringApplicationBuilder(ServerlessAuthApplication.class)
                                    .web(WebApplicationType.NONE)
                                    .run();
                }
            }
        }
        return context;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return getContext().getBean(beanClass);
    }

    public static void closeContext() {
        synchronized (lock) {
            if (context != null) {
                context.close();
                context = null;
            }
        }
    }
}
