# AWS Lambda OpenTracing Java SDK

This SDK provides Open Tracing instrumentation for AWS Lambda. Releases for this project will be on an "as needed" basis. As new features of Lambda come out we will evaluate them and if useful will add support for them here.

Versioning will have the following format: {majorVersion}.{minorVersion}.{pointVersion} 

### How it Works

The SDK provides [`TracingRequestHandler`](https://github.com/aws/aws-lambda-java-libs/blob/master/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestHandler.java) and [`TracingRequestStreamHandler`](https://github.com/aws/aws-lambda-java-libs/blob/master/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestStreamHandler.java) interfaces that extend AWS' Lambda request handlers. When a Lambda function that is using an implementation of either tracing request handler is invoked, the handler will obtain the globally registered OpenTracing [Tracer](https://opentracing.io/docs/overview/tracers/) and create/start an OpenTracing [Span](https://opentracing.io/docs/overview/spans/) to capture timing information and `key:value` pairs ([Tags/Logs](https://opentracing.io/docs/overview/tags-logs-baggage/)) detailing the trace data.

As part of the implementation the user must override the tracing handler's `doHandleRequest` method which is called by the handler interface's `handleRequest` method.

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
You can either build the project locally as described below and then add the jar dependency into your gradle (or maven) file:
```groovy
dependencies {
    compile files('/path/to/java-aws-lambda.jar')
}
```
Or you can add the dependency like so:
```
dependencies {
    // TODO verify this is correct when released
    implementation "com.newrelic.opentracing.aws:java-aws-lambda:X.X.X"
}
```

#### Implement
1. Implement the `TracingRequestHandler` interface as shown in the [example](#example-usage), defining the Lambda function input and output types that your function requires.
2. Override the `doHandleRequest` method from the interface and execute your function logic within it. *Note:* If you are refactoring a pre-existing Lambda handler to take advantage of this SDK's tracing request handler make sure that it overrides `doHandleRequest` but not `handleRequest`.
3. Register the OpenTracing Tracer of your choice.
4. See Amazon's documentation on [creating a ZIP deployment package for a Java Lambda function](https://docs.aws.amazon.com/lambda/latest/dg/create-deployment-pkg-zip-java.html)
5. When creating your Lambda function in AWS Lambda console the handler for the given example would be entered as `com.handler.example.MyLambdaHandler::handleRequest` or just `com.handler.example.MyLambdaHandler`, the latter of which will use `handleRequest` as the handler method by default. *Note:* `handleRequest` is used as the handler entry point as it will call `doHandleRequest`.

#### Example Usage

```java
package com.handler.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.newrelic.opentracing.aws.TracingRequestHandler;
import io.opentracing.util.GlobalTracer;

import java.util.Map;

/**
 * Tracing request handler that creates a span on every invocation of a Lambda.
 *
 * @param Map<String, Object> The Lambda Function input
 * @param String The Lambda Function output
 */
public class MyLambdaHandler implements TracingRequestHandler<Map<String, Object>, String> {
    static {
        // TODO Obtain an instance of the OpenTracing Tracer of your choice
        Tracer tracer = new CustomTracer(...);
        // Register your tracer as the Global Tracer
        GlobalTracer.register(tracer);
    }

    /**
     * Method that handles the Lambda function request.
     *
     * @param input The Lambda Function input
     * @param context The Lambda execution environment context object
     * @return String The Lambda Function output
     */
    @Override
    public String doHandleRequest(Map<String, Object> input, Context context) {
        // TODO Your function logic here
        return "Lambda Function output";
    }
}
```


## Getting Started

### Requirements

* AWS Lambda instance
* Java 8
* Gradle

### Building
Run the following gradle task: 
```
./gradlew java-aws-lambda:jar
```  

This generates a jar artifact in the following subdirectory:
```
java-aws-lambda/java-aws-lambda/build/libs/java-aws-lambda.jar
```

### Testing
Run the following gradle task: 
```
./gradlew test
```

### Contributing
Full details are available in our CONTRIBUTING file;

We'd love to get your contributions to improve AWS Lambda OpenTracing Java SDK! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. If you'd like to execute our corporate CLA, or if you have any questions, please drop us an email at open-source@newrelic.com.

### Licensing
The AWS Lambda OpenTracing Java SDK is licensed under the Apache 2.0 License.

The AWS Lambda OpenTracing Java SDK also uses source code from third party libraries. Full details on which libraries are used and the terms under which they are licensed can be found in the third party notices document.
