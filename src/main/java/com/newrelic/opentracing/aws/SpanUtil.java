/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SpanUtil {

  private SpanUtil() {}

  static <Input> Span buildSpan(
      Input input, Context context, Tracer tracer, SpanContext spanContext) {
    return EnhancedSpanBuilder.basedOn(tracer, "handleRequest")
        .asChildOf(spanContext)
        .withTag("aws.requestId", context.getAwsRequestId())
        .withTag("aws.lambda.arn", context.getInvokedFunctionArn())
        .optionallyWithTag(
            "aws.lambda.eventSource.arn", EventSourceParser.parseEventSourceArn(input))
        .optionallyWithTag(
            "aws.lambda.coldStart", TracingRequestHandler.isColdStart.getAndSet(false))
        .start();
  }

  public static Map<String, Object> createErrorAttributes(Throwable throwable) {
    final Map<String, Object> errorAttributes = new HashMap<>();
    errorAttributes.put("event", Tags.ERROR.getKey());
    errorAttributes.put("error.object", throwable);
    errorAttributes.put("message", throwable.getMessage());
    errorAttributes.put("stack", throwable.getStackTrace());
    errorAttributes.put("error.kind", "Exception");
    return Collections.unmodifiableMap(errorAttributes);
  }
}
