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
