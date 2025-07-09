package com.TrashTDL.ServerlessAuth.config;

import com.TrashTDL.ServerlessAuth.ServerlessAuthApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.HashMap;
import java.util.Map;

public class SpringContextHolder {
    private static ConfigurableApplicationContext context;
    private static final Object lock = new Object();

    public static ConfigurableApplicationContext getContext() {
        if (context == null) {
            synchronized (lock) {
                if (context == null) {
                    // log JDBC URL
                    System.out.println("JDBC URL: " + System.getenv("SPRING_DATASOURCE_URL"));
                    Map<String, Object> props = new HashMap<>();
                    props.put("spring.datasource.url", System.getenv("SPRING_DATASOURCE_URL"));
                    props.put("spring.datasource.username", System.getenv("SPRING_DATASOURCE_USERNAME"));
                    props.put("spring.datasource.password", System.getenv("SPRING_DATASOURCE_PASSWORD"));

                    context = new SpringApplicationBuilder(ServerlessAuthApplication.class)
                                    .web(WebApplicationType.NONE)
                                    .properties(props)
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
