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

import com.amazonaws.services.lambda.runtime.Context;
import io.opentracing.Scope;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpanUtil {

  private SpanUtil() {}

  public static <Input> void setTags(
      Scope scope, Context context, Input input, AtomicBoolean isColdStart) {
    scope.span().setTag("aws.requestId", context.getAwsRequestId());
    scope.span().setTag("aws.lambda.arn", context.getInvokedFunctionArn());

    final String sourceArn = EventSourceParser.parseEventSourceArn(input);
    if (sourceArn != null) {
      scope.span().setTag("aws.lambda.eventSource.arn", sourceArn);
    }

    if (isColdStart.getAndSet(false)) {
      scope.span().setTag("aws.lambda.coldStart", true);
    }
  }

  public static Map<String, Object> createErrorAttributes(Throwable throwable) {
    final Map<String, Object> errorAttributes = new HashMap<>();
    errorAttributes.put("event", Tags.ERROR.getKey());
    errorAttributes.put("error.object", throwable);
    errorAttributes.put("message", throwable.getMessage());
    errorAttributes.put("stack", throwable.getStackTrace());
    errorAttributes.put("error.kind", "Exception");
    return errorAttributes;
  }
}
