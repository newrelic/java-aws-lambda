package io.opentracing.contrib.aws;

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
