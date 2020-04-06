package com.newrelic.opentracing.aws;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

public class EnhancedSpanBuilder {
  private final Tracer.SpanBuilder spanBuilder;

  public static EnhancedSpanBuilder basedOn(Tracer tracer, String operationName) {
    return new EnhancedSpanBuilder(tracer.buildSpan(operationName));
  }

  private EnhancedSpanBuilder(Tracer.SpanBuilder innerSpanBuilder) {
    spanBuilder = innerSpanBuilder;
  }

  public EnhancedSpanBuilder asChildOf(SpanContext spanContext) {
    this.spanBuilder.asChildOf(spanContext);
    return this;
  }

  /** Same as {@link Span#setTag(String, String)}, but for the span to be built. */
  EnhancedSpanBuilder withTag(String key, String value) {
    this.spanBuilder.withTag(key, value);
    return this;
  }

  /** Same as {@link Span#setTag(String, boolean)}, but for the span to be built. */
  EnhancedSpanBuilder withTag(String key, boolean value) {
    this.spanBuilder.withTag(key, value);
    return this;
  }

  /** Same as {@link Span#setTag(String, Number)}, but for the span to be built. */
  EnhancedSpanBuilder withTag(String key, Number value) {
    this.spanBuilder.withTag(key, value);
    return this;
  }

  /**
   * A shorthand for withTag("key", "value").
   *
   * <p>If parent==null, this is a noop.
   */
  EnhancedSpanBuilder optionallyWithTag(String key, String value) {
    if (value != null) {
      this.spanBuilder.withTag(key, value);
    }
    return this;
  }

  EnhancedSpanBuilder optionallyWithTag(String key, boolean value) {
    if (value) {
      this.spanBuilder.withTag(key, true);
    }
    return this;
  }

  EnhancedSpanBuilder optionallyWithTag(String key, Number value) {
    if (value != null) {
      this.spanBuilder.withTag(key, value);
    }
    return this;
  }

  public Span start() {
    return this.spanBuilder.start();
  }
}
