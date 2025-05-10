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
        return req.createResponseBuilder(HttpStatus.OK)
                  .body("SimpleTest OK")
                  .build();
    }
}
