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
        String responseBody = "SimpleTest OK. SPRING_DATASOURCE_URL: " + (springDatasourceUrl == null ? "NULL" : springDatasourceUrl);

        // Log to context logger as well, in case it's more visible
        if (context != null && context.getLogger() != null) {
            context.getLogger().info("SPRING_DATASOURCE_URL from SimpleTestFunction: " + (springDatasourceUrl == null ? "NULL" : springDatasourceUrl));
        }

        return req.createResponseBuilder(HttpStatus.OK)
                  .body(responseBody)
                  .build();
    }
}
