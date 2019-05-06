# AWS Lambda OpenTracing Java SDK

## Purpose

Open Tracing instrumentation for AWS Lambda [RequestHandler](https://github.com/aws/aws-lambda-java-libs/blob/master/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestHandler.java) and [RequestStreamHandler](https://github.com/aws/aws-lambda-java-libs/blob/master/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestStreamHandler.java).

## How it Works

The SDK provides `TracingRequestHandler` and `TracingRequestStreamHandler` interfaces that extend AWS' Lambda request handlers. When a Lambda function that is using an implementation of either tracing request handler is invoked, the handler will obtain the globally registered OpenTracing [Tracer](https://opentracing.io/docs/overview/tracers/) and create/start an OpenTracing [Span](https://opentracing.io/docs/overview/spans/) to capture timing information and `key:value` pairs ([Tags/Logs](https://opentracing.io/docs/overview/tags-logs-baggage/)) detailing the trace data.

As part of the implementation the user must override the tracing handler's `doHandleRequest` method which is called by the handler interface's `handleRequest` method.

## Collected Span Tags/Logs

Lambda attributes:

| Tag key                          | Tag type  |                        Note                                       |
| :------------------------------: | :-------: | :---------------------------------------------------------------: |
| `aws.requestId`                  | `String`  | AWS Request ID is a unique GUID assigned to each invocation       |
| `aws.lambda.arn`                 | `String`  | ARN of the current Lambda function                                |
| `aws.lambda.eventSource.arn`     | `String`  | ARN of the invocation source                                      |
| `aws.lambda.coldStart`           | `boolean` | Indicates if Lambda invocation was a cold start, omitted if false |

Exception log:

| Log key        | Log type                |                        Note                      |
| :------------: | :---------------------: | :----------------------------------------------: |
| `event`        | `String` `"error"`      | Indicates that an error event has occurred       |
| `error.object` | `Throwable`             | The `Throwable` object                           |
| `message`      | `Throwable` message     | The detail message string of the throwable       |
| `stack`        | `Throwable` stacktrace  | The the stack trace information of the throwable |
| `error.kind`   | `String` `"Exception"`  | Indicates that the error was an `Exception`      |

## How to Use

1. Add the `java-aws-lambda` dependency to your project.
    - **Option A:** [Build the project from sources](#build-the-project) and [add the jar to your project as a Gradle dependency](#add-artifact-to-gradle-project)
    - **Option B:** Add gradle or maven dependency to your project: [TODO N/A for Beta](https://mvnrepository.com)
2. Implement the `TracingRequestHandler` interface as shown in the [example](#example-usage), defining the Lambda function input and output types that your function requires.
3. Override the `doHandleRequest` method from the interface and execute your function logic within it. *Note:* If you are refactoring a pre-existing Lambda handler to take advantage of this SDK's tracing request handler make sure that it overrides `doHandleRequest` but not `handleRequest`.
4. Register the OpenTracing Tracer of your choice (e.g. New Relic, Jaeger, etc).
5. See Amazon's documentation on [creating a ZIP deployment package for a Java Lambda function](https://docs.aws.amazon.com/lambda/latest/dg/create-deployment-pkg-zip-java.html)
6. When creating your Lambda function in AWS Lambda console the handler for the given example would be entered as `com.handler.example.MyLambdaHandler::handleRequest` or just `com.handler.example.MyLambdaHandler`, the latter of which will use `handleRequest` as the handler method by default. *Note:* `handleRequest` is used as the handler entry point as it will call `doHandleRequest`.

## Build the Project

Run jar task: `./gradlew jar`

Artifact: `java-aws-lambda/build/libs/java-aws-lambda.jar`

## Add Artifact to Gradle Project

Include the jar by adding it as a dependency in your `build.gradle` file:

```groovy
dependencies {
    compile files('/path/to/java-aws-lambda.jar')
}
```

## Example Usage

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
