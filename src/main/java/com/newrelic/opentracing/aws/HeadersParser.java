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

import com.amazonaws.Request;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import java.util.Map;

final class HeadersParser {

  private HeadersParser() {}

  static <Input> SpanContext parseAndExtract(Tracer tracer, Input input) {
    try {
      if (input instanceof Map) {
        Map map = (Map) input;
        /*
         * New Relic distributed tracing headers can be directly passed as Input to an AWS Lambda function via the
         * com.amazonaws.services.lambda.model.InvokeRequest#withPayload method as a String.
         *
         * Alternatively, a Map of propagated headers containing the "newrelic" distributed tracing payload can be parsed.
         */
        if (map.get("newrelic") != null) {
          return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(map));
        } else {
          final Object headers = map.get("headers");
          if (headers instanceof Map) {
            final Map<String, String> headerStr = (Map<String, String>) headers;
            return tracer.extract(
                Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headerStr));
          }
        }
      } else if (input instanceof com.amazonaws.Request) {
        final Request request = (Request) input;
        final Map<String, String> headers = request.getHeaders();
        return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
      }
    } catch (IllegalArgumentException exception) {
    }
    return null;
  }
}
