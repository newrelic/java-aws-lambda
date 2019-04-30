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

package io.opentracing;

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
