package com.ToDoList.auth.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApplicationContextProvider {
    private static final ApplicationContext context =
        new AnnotationConfigApplicationContext(ApplicationConfig.class);

    //@SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> clazz) {
        return (T) context.getBean(clazz);
    }
}