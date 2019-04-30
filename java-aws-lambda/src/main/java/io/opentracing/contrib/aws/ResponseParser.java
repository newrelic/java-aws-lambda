package io.opentracing.contrib.aws;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.opentracing.Span;
import java.util.Map;

public class ResponseParser {

  private ResponseParser() {}

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
    }

    if (statusCode != null && !statusCode.isEmpty()) {
      span.setTag("http.status_code", statusCode);
    }
  }
}
