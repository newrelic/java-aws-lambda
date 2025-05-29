package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

/**
 * Trace calls to lambda functions, for arbitrary Input and Output types.
 *
 * <p>For flexibility, applications may extend this class to enhance the root span or handle novel
 * invocation event types.
 *
 * @param <Input>  The invocation payload type for your lambda function.
 * @param <Output> The result type for your lambda function.
 */
public class LambdaTracing<Input, Output> {
    protected static final AtomicBoolean isColdStart = new AtomicBoolean(true);

    /**
     * One-line instrumentation convenience method.
     *
     * @param input       The invocation event
     * @param context     The invocation context
     * @param realHandler The callback that implements the business logic for this event handler
     * @param <Input>     The type of the invocation event
     * @param <Output>    The type of the response
     * @return The invocation response (the return value of the realHandler callback)
     */
    public static <Input, Output> Output instrument(
            Input input, Context context, BiFunction<Input, Context, Output> realHandler) {
        return new LambdaTracing<Input, Output>().instrumentRequest(input, context, realHandler);
    }

    /**
     * Instrument a Lambda invocation
     *
     * @param input       The invocation event
     * @param context     The invocation context
     * @param realHandler The function that implements the business logic. Will be invoked with the
     *                    input and context parameters, from within the instrumentation scope.
     * @return the return value from realHandler
     */
    public Output instrumentRequest(
            Input input, Context context, BiFunction<Input, Context, Output> realHandler) {
        final Tracer tracer = GlobalTracer.get();
        final SpanContext spanContext = extractContext(tracer, input);

        Span span = buildRootSpan(input, context, tracer, spanContext);
        try (Scope scope = tracer.activateSpan(span)) {
            Output output = realHandler.apply(input, context);
            parseResponse(span, output);
            return output;
        } catch (Throwable throwable) {
            span.log(SpanUtil.createErrorAttributes(throwable));
            throw throwable;
        } finally {
            span.finish();
        }
    }

    protected SpanContext extractContext(Tracer tracer, Object input) {
        return HeadersParser.parseAndExtract(tracer, input);
    }

    protected Span buildRootSpan(
            Input input, Context context, Tracer tracer, SpanContext spanContext) {
        return SpanUtil.buildSpan(input, context, tracer, spanContext, isColdStart);
    }

    protected void parseResponse(Span span, Output output) {
        ResponseParser.parseResponse(output, span);
    }
}
