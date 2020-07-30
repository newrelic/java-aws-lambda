package com.newrelic.opentracing.aws;

import static org.junit.Assert.assertEquals;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class ReflectionTest {
  private static final int SYNTHETIC_MODIFIER = 0x1000;

  private Object handler;

  @Before
  public void setup() {
    handler = new TestHandler();
  }

  @Test
  public void testInputReflection() {
    // Ignoring synthetics, we expect handleRequest to take the declared type as its first arg.
    // This is necessary for correct payload deserialization.
    final Method handleRequest =
        Arrays.stream(handler.getClass().getMethods())
            .filter(
                m ->
                    ((m.getModifiers() & SYNTHETIC_MODIFIER) == 0)
                        && m.getName().equals("handleRequest"))
            .findFirst()
            .orElseThrow(AssertionError::new);

    assertEquals(APIGatewayProxyRequestEvent.class, handleRequest.getParameterTypes()[0]);
  }

  public static class TestHandler
      implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
        APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
      return LambdaTracing.instrument(
          apiGatewayProxyRequestEvent, context, (event, c) -> new APIGatewayProxyResponseEvent());
    }
  }
}
