package io.opentracing.contrib.aws;

import com.amazonaws.Request;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

import java.util.Map;

final class HeadersParser {

    private HeadersParser() {
    }

    static <Input> SpanContext parseAndExtract(Tracer tracer, Input input) {
        try {
            if (input instanceof Map) {
                Map map = (Map) input;
                final Object headers = map.get("headers");
                if (headers instanceof Map) {
                    final Map<String, String> headerStr = (Map<String, String>) headers;
                    return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headerStr));
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
