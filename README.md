# AWS Lambda OpenTracing Java SDK

This SDK provides Open Tracing instrumentation for AWS Lambda. Releases for this project will be on an "as needed" basis. As new features of Lambda come out we will evaluate them and if useful will add support for them here.

Versioning will have the following format: {majorVersion}.{minorVersion}.{pointVersion} 

### Supported OpenTracing Versions

* OpenTracing 0.31.0: [com.newrelic.opentracing:java-aws-lambda:1.0.0](https://mvnrepository.com/artifact/com.newrelic.opentracing/java-aws-lambda/1.0.0)
* OpenTracing 0.33.0: [com.newrelic.opentracing:java-aws-lambda:2.0.0](https://mvnrepository.com/artifact/com.newrelic.opentracing/java-aws-lambda/2.0.0)

### How it Works

The SDK provides `LambdaTracing` and `StreamLambdaTracing` classes that instrument requests. When a Lambda function 
that is using an implementation of either class is invoked, the handler will obtain the globally registered OpenTracing 
[Tracer](https://opentracing.io/docs/overview/tracers/) and create/start an OpenTracing 
[Span](https://opentracing.io/docs/overview/spans/) to capture timing information and `key:value` pairs 
([Tags/Logs](https://opentracing.io/docs/overview/tags-logs-baggage/)) detailing the trace data.

As part of the implementation the user's handler must call the `instrument` method, passing a callback that contains 
the business logic for their handler.


### Collected Span Tags/Logs

Below are a list of the collected Lambda attributes:

| Tag key                          | Tag type  |                        Note                                       |
| :------------------------------: | :-------: | :---------------------------------------------------------------: |
| `aws.requestId`                  | `String`  | AWS Request ID is a unique GUID assigned to each invocation       |
| `aws.lambda.arn`                 | `String`  | ARN of the current Lambda function                                |
| `aws.lambda.eventSource.arn`     | `String`  | ARN of the invocation source                                      |
| `aws.lambda.coldStart`           | `boolean` | Indicates if Lambda invocation was a cold start, omitted if false |

Below are a list of the collected exception attributes:

| Log key        | Log type                |                        Note                      |
| :------------: | :---------------------: | :----------------------------------------------: |
| `event`        | `String` `"error"`      | Indicates that an error event has occurred       |
| `error.object` | `Throwable`             | The `Throwable` object                           |
| `message`      | `Throwable` message     | The detail message string of the throwable       |
| `stack`        | `Throwable` stacktrace  | The the stack trace information of the throwable |
| `error.kind`   | `String` `"Exception"`  | Indicates that the error was an `Exception`      |

### How to Use

#### Add Dependency
You can add the dependency by adding the following to your `build.gradle` file:
```
dependencies {
    implementation "com.newrelic.opentracing:java-aws-lambda:2.1.0"
}
```

#### Implement
1. Implement the `TracingRequestHandler` interface as shown in the [example](#example-usage), defining the Lambda function input and output types that your function requires.
2. Override the `doHandleRequest` method from the interface and execute your function logic within it. *Note:* If you are refactoring a pre-existing Lambda handler to take advantage of this SDK's tracing request handler make sure that it overrides `doHandleRequest` but not `handleRequest`.
3. Register the OpenTracing Tracer of your choice (e.g. New Relic, Jaeger, etc).
4. See Amazon's documentation on [creating a ZIP deployment package for a Java Lambda function](https://docs.aws.amazon.com/lambda/latest/dg/create-deployment-pkg-zip-java.html)
5. When creating your Lambda function in AWS Lambda console the handler for the given example would be entered as `com.handler.example.MyLambdaHandler::handleRequest` or just `com.handler.example.MyLambdaHandler`, the latter of which will use `handleRequest` as the handler method by default. *Note:* `handleRequest` is used as the handler entry point as it will call `doHandleRequest`.

#### Example Usage

```java
package com.handler.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.newrelic.opentracing.LambdaTracer;
import com.newrelic.opentracing.aws.LambdaTracing;
import io.opentracing.util.GlobalTracer;

import java.util.Map;

public static class YourLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static {
        // Obtain an instance of the OpenTracing Tracer of your choice
        Tracer tracer = LambdaTracer.INSTANCE;
        // Register your tracer as the Global Tracer
        GlobalTracer.registerIfAbsent(tracer);
    }
 
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        return LambdaTracing.instrument(apiGatewayProxyRequestEvent, context, (event, ctx) -> {
            // Your business logic here
            return doSomethingWithTheEvent(event);
        });
    }
}
```

## Getting Started

### Requirements

* AWS Lambda instance
* Java 8
* Gradle

### Building
Run the following command: 
```
./gradlew jar
```  

This generates a jar artifact in the following subdirectory:
```
java-aws-lambda/build/libs/java-aws-lambda.jar
```

### Testing
Run the following gradle task: 
```
./gradlew test
```

### Contributing
Full details are available in our [CONTRIBUTING.md](CONTRIBUTING.md) file.

We'd love to get your contributions to improve AWS Lambda OpenTracing Java SDK! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project.

To execute our corporate CLA, which is required if your contribution is on behalf of a company, or if you have any questions, please drop us an email at open-source@newrelic.com. 

### Licensing
The AWS Lambda OpenTracing Java SDK is licensed under the Apache 2.0 License.
