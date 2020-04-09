/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.Request;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import java.util.Map;

final class HeadersParser {

  private HeadersParser() {}

  static <Input> SpanContext parseAndExtract(Tracer tracer, Input input) {
    try {
      if (input instanceof Map) {
        Map map = (Map) input;
        final Object headers = map.get("headers");
        if (headers instanceof Map) {
          final Map<String, String> headerStr = (Map<String, String>) headers;
          return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(headerStr));
        }
      } else if (input instanceof com.amazonaws.Request) {
        final Request request = (Request) input;
        final Map<String, String> headers = request.getHeaders();
        return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(headers));
      }
    } catch (IllegalArgumentException exception) {
    }
    return null;
  }
}
