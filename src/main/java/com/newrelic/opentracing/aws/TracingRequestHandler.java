/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

/**
 * Tracing request handler that creates a span on every invocation of a Lambda.
 *
 * <p>Implement this interface and update your AWS Lambda Handler name to reference your class name,
 * e.g., com.mycompany.HandlerClass
 *
 * <p>Due to an interaction between Java's type erasure and method inheritance, Input effectively
 * must be a Map. For that reason, this interface is deprecated in favor of {@link LambdaTracing}.
 *
 * @param <Input>  The input parameter type
 * @param <Output> The output parameter type
 */
@Deprecated
public interface TracingRequestHandler<Input, Output>
        extends com.amazonaws.services.lambda.runtime.RequestHandler<Input, Output> {

    /**
     * Method that handles the Lambda function request.
     *
     * <p>Override this method in your code.
     *
     * @param input   The Lambda Function input
     * @param context The Lambda execution environment context object
     * @return The Lambda Function output
     */
    Output doHandleRequest(Input input, Context context);

    default Output handleRequest(Input input, Context context) {
        return LambdaTracing.instrument(input, context, this::doHandleRequest);
    }

    /**
     * Override to extract context from Input.
     *
     * <p>Implementations should call {@link Tracer#extract(Format, Object)} and return the extracted
     * SpanContext.
     *
     * @param tracer OpenTracing tracer
     * @param input  input to Lambda function
     * @return SpanContext extracted from input, null if there was no context or there was an issue
     * extracting this context
     */
    default SpanContext extractContext(Tracer tracer, Input input) {
        return HeadersParser.parseAndExtract(tracer, input);
    }
}
