package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Trace calls to lambda functions, implementing manual JSON serialization.
 *
 * <p>For flexibility, applications may extend this class to enhance the root span.
 */
public class StreamLambdaTracing {
  /**
   * One-line instrumentation convenience method.
   *
   * @param input The invocation event's input stream
   * @param output The invocation response output stream
   * @param context The invocation context
   * @param realHandler The callback that implements the business logic for this event handler
   */
  public static void instrument(
      InputStream input, OutputStream output, Context context, RequestStreamHandler realHandler)
      throws IOException {
    new StreamLambdaTracing().instrumentRequest(input, output, context, realHandler);
  }

  /**
   * Instrument a Lambda invocation
   *
   * @param input The invocation event's input stream
   * @param output The invocation response output stream
   * @param context The invocation context
   * @param realHandler The function that implements the business logic. Will be invoked with the
   *     input and context parameters, from within the instrumentation scope.
   */
  public void instrumentRequest(
      InputStream input, OutputStream output, Context context, RequestStreamHandler realHandler)
      throws IOException {
    final Tracer tracer = GlobalTracer.get();
    final SpanContext spanContext = extractContext(tracer, input);

    Span span = buildRootSpan(input, context, tracer, spanContext);
    try (Scope scope = tracer.activateSpan(span)) {
      try {
        realHandler.handleRequest(input, output, context);
      } catch (Throwable throwable) {
        span.log(SpanUtil.createErrorAttributes(throwable));
        throw throwable;
      }
    } finally {
      span.finish();
    }
  }

  protected Span buildRootSpan(
      InputStream input, Context context, Tracer tracer, SpanContext spanContext) {
    return SpanUtil.buildSpan(input, context, tracer, spanContext, LambdaTracing.isColdStart);
  }

  protected SpanContext extractContext(Tracer tracer, InputStream input) {
    return null;
  }
}
