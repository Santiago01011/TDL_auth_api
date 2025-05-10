package com.TrashTDL.ServerlessAuth.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

public class SimpleTestFunction {

    @FunctionName("SimpleTest")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req",
                     methods = { HttpMethod.GET },
                     route = "simple/test")
        HttpRequestMessage<Void> req,
        final ExecutionContext context
    ) {
        String springDatasourceUrl = System.getenv("SPRING_DATASOURCE_URL");
        String jwtSecretKey = System.getenv("JWT_SECRET_KEY");

        String responseBody = String.format("SimpleTest OK. SPRING_DATASOURCE_URL: %s, JWT_SECRET_KEY: %s",
                                (springDatasourceUrl == null ? "NULL" : springDatasourceUrl),
                                (jwtSecretKey == null ? "NULL" : jwtSecretKey));

        // Log to context logger as well, in case it's more visible
        if (context != null && context.getLogger() != null) {
            context.getLogger().info("SPRING_DATASOURCE_URL from SimpleTestFunction: " + (springDatasourceUrl == null ? "NULL" : springDatasourceUrl));
            context.getLogger().info("JWT_SECRET_KEY from SimpleTestFunction: " + (jwtSecretKey == null ? "NULL" : jwtSecretKey));
        }

        return req.createResponseBuilder(HttpStatus.OK)
                  .body(responseBody)
                  .build();
    }
}
