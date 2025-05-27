/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;

import java.util.HashMap;
import java.util.List;
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
                    return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(headerStr));
                }
            } else if (input instanceof APIGatewayV2HTTPEvent) {
                return tracer.extract(Format.Builtin.HTTP_HEADERS,
                        new TextMapAdapter(((APIGatewayV2HTTPEvent) input).getHeaders()));
            } else if (input instanceof APIGatewayProxyRequestEvent) {
                return tracer.extract(Format.Builtin.HTTP_HEADERS,
                        new TextMapAdapter(((APIGatewayProxyRequestEvent) input).getHeaders()));
            } else if (input instanceof ApplicationLoadBalancerRequestEvent) {
                return tracer.extract(Format.Builtin.HTTP_HEADERS,
                        new TextMapAdapter(((ApplicationLoadBalancerRequestEvent) input).getHeaders()));
            } else if (input instanceof SNSEvent) {
                SNSEvent snsEvent = (SNSEvent) input;
                List<SNSEvent.SNSRecord> records = snsEvent.getRecords();
                Map<String, String> extractedHeaders = new HashMap<>();
                if (!records.isEmpty()) {
                    Map<String, SNSEvent.MessageAttribute> messageAttributes = records.get(0).getSNS().getMessageAttributes();

                    if (messageAttributes != null) {
                        for (Map.Entry<String, SNSEvent.MessageAttribute> entry : messageAttributes.entrySet()) {
                            extractedHeaders.put(entry.getKey(), entry.getValue().getValue());
                        }
                    }
                }

                return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(extractedHeaders));
            } else if (input instanceof SQSEvent) {
                SQSEvent sqsEvent = (SQSEvent) input;
                List<SQSEvent.SQSMessage> records = sqsEvent.getRecords();
                Map<String, String> extractedHeaders = new HashMap<>();
                if (!records.isEmpty()) {
                    Map<String, SQSEvent.MessageAttribute> messageAttributes = records.get(0).getMessageAttributes();

                    if (messageAttributes != null) {
                        for (Map.Entry<String, SQSEvent.MessageAttribute> entry : messageAttributes.entrySet()) {
                            extractedHeaders.put(entry.getKey(), entry.getValue().getStringValue());
                        }
                    }
                }

                return tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(extractedHeaders));
            }
        } catch (IllegalArgumentException exception) {
        }
        return null;
    }
}
