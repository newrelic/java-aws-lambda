/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import io.opentracing.Span;

import java.util.Map;

public class ResponseParser {

    private ResponseParser() {
    }

    /**
     * Attempt to parse a status code from the response object, which could be present if the event
     * source type was created from an Application Load Balancer or API Gateway.
     */
    public static <Output> void parseResponse(Output response, Span span) {
        String statusCode = null;

        if (response instanceof Map) {
            Map map = (Map) response;
            Object statusCodeObject = map.get("statusCode");
            if (statusCodeObject instanceof String) {
                statusCode = (String) statusCodeObject;
            } else if (statusCodeObject instanceof Number) {
                statusCode = ((Number) statusCodeObject) + "";
            }
        } else if (response instanceof APIGatewayProxyResponseEvent) {
            final APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                    (APIGatewayProxyResponseEvent) response;
            if (response != null) {
                statusCode = apiGatewayProxyResponseEvent.getStatusCode() + "";
            }
        } else if (response instanceof APIGatewayV2ProxyResponseEvent) {
            final APIGatewayV2ProxyResponseEvent apiGatewayV2ProxyResponseEvent =
                    (APIGatewayV2ProxyResponseEvent) response;
            if (response != null) {
                statusCode = apiGatewayV2ProxyResponseEvent.getStatusCode() + "";
            }
        }

        if (statusCode != null && !statusCode.isEmpty()) {
            span.setTag("http.status_code", statusCode);
        }
    }
}
