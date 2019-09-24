/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import java.lang.reflect.Field;

public class GlobalTracerTestUtils {

  public static void initTracer(Tracer tracer) {
    try {
      Field globalTracerField = GlobalTracer.class.getDeclaredField("tracer");
      globalTracerField.setAccessible(true);
      globalTracerField.set(null, tracer);
      globalTracerField.setAccessible(false);
    } catch (Exception e) {
      throw new RuntimeException("Unable to initialize tracer: " + e);
    }
  }
}
