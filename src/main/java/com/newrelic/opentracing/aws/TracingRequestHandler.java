/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracing request handler that creates a span on every invocation of a Lambda.
 *
 * <p>Implement this interface and update your AWS Lambda Handler name to reference your class name,
 * e.g., com.mycompany.HandlerClass
 *
 * @param <Input> The input parameter type
 * @param <Output> The output parameter type
 */
public interface TracingRequestHandler<Input, Output>
    extends com.amazonaws.services.lambda.runtime.RequestHandler<Input, Output> {

  AtomicBoolean isColdStart = new AtomicBoolean(true);

  /**
   * Method that handles the Lambda function request.
   *
   * <p>Override this method in your code.
   *
   * @param input The Lambda Function input
   * @param context The Lambda execution environment context object
   * @return The Lambda Function output
   */
  Output doHandleRequest(Input input, Context context);

  default Output handleRequest(Input input, Context context) {
    final Tracer tracer = GlobalTracer.get();
    final SpanContext spanContext = extractContext(tracer, input);

    try (Scope scope = tracer.buildSpan("handleRequest").asChildOf(spanContext).startActive(true)) {
      SpanUtil.setTags(scope, context, input, isColdStart);
      try {
        Output output = doHandleRequest(input, context);
        ResponseParser.parseResponse(output, scope.span());
        return output;
      } catch (Throwable throwable) {
        scope.span().log(SpanUtil.createErrorAttributes(throwable));
        throw throwable;
      }
    }
  }

  /**
   * Override to extract context from Input.
   *
   * <p>Implementations should call {@link Tracer#extract(Format, Object)} and return the extracted
   * SpanContext.
   *
   * @param tracer OpenTracing tracer
   * @param input input to Lambda function
   * @return SpanContext extracted from input, null if there was no context or there was an issue
   *     extracting this context
   */
  default SpanContext extractContext(Tracer tracer, Input input) {
    return HeadersParser.parseAndExtract(tracer, input);
  }
}
