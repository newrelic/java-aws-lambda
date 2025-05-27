/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import io.opentracing.References;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.propagation.TextMapAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static io.opentracing.propagation.Format.Builtin.TEXT_MAP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class EnhancedSpanBuilderTest {

    private Random random = new Random();

    private MockTracer tracer;
    private MockSpan.MockContext context;

    @Before
    public void beforeEach() {
        Map<String, String> parentIds = new HashMap<>();
        parentIds.put("traceid", Math.abs(this.random.nextLong()) + "");
        parentIds.put("spanid", Math.abs(this.random.nextLong()) + "");

        this.tracer = new MockTracer();
        this.context = (MockSpan.MockContext) tracer.extract(TEXT_MAP, new TextMapAdapter(parentIds));
    }

    @After
    public void afterEach() {
        this.tracer.close();
        this.tracer.reset();
    }

    @Test
    public void basedOn() {
        MockSpan builtSpan = (MockSpan) EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation").start();
        assertThat(builtSpan.operationName(), is(equalTo("AnOperation")));
        assertThat(builtSpan.tags().entrySet(), hasSize(0));
    }

    @Test
    public void asChildOf() {
        MockSpan.Reference expectedReference =
                new MockSpan.Reference(this.context, References.CHILD_OF);

        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation").asChildOf(this.context).start();

        assertThat(builtSpan.operationName(), is(equalTo("AnOperation")));
        assertThat(builtSpan.references(), hasSize(1));
        assertThat(builtSpan.references(), contains(expectedReference));
        assertThat(builtSpan.tags().entrySet(), hasSize(0));
    }

    @Test
    public void withTagStringKeyValue() {
        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation")
                                .withTag("ATag", "SomeValue")
                                .start();

        assertThat(builtSpan.tags(), hasEntry("ATag", "SomeValue"));
    }

    @Test
    public void withTagBooleanValues() {
        boolean expectedValue = this.random.nextBoolean();
        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation")
                                .withTag("ABooleanKey", expectedValue)
                                .start();

        assertThat(builtSpan.tags().entrySet(), hasSize(1));
        assertThat(builtSpan.tags(), hasEntry("ABooleanKey", expectedValue));
    }

    @Test
    public void withTagNumberValues() {
        Integer expectedValue = this.random.nextInt();
        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation")
                                .withTag("ANumberKey", expectedValue)
                                .start();

        assertThat(builtSpan.tags().entrySet(), hasSize(1));
        assertThat(builtSpan.tags(), hasEntry("ANumberKey", expectedValue));
    }

    @Test
    public void optionallyWithTagDoesNotSetWithNullStrings() {
        String expectedValue = null;
        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation")
                                .optionallyWithTag("ATagKey", expectedValue)
                                .start();

        assertThat(builtSpan.tags().entrySet(), hasSize(0));
    }

    @Test
    public void optionallyWithTagStringKeyValue() {
        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation")
                                .optionallyWithTag("ATag", "SomeValue")
                                .start();

        assertThat(builtSpan.tags(), hasEntry("ATag", "SomeValue"));
    }

    @Test
    public void optionallyWithTagDoesNotSetWithNullNumbers() {
        Number expectedValue = null;
        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation")
                                .optionallyWithTag("ATagKey", expectedValue)
                                .start();

        assertThat(builtSpan.tags().entrySet(), hasSize(0));
    }

    @Test
    public void optionallyWithTagNumberValues() {
        Integer expectedValue = this.random.nextInt();
        MockSpan builtSpan =
                (MockSpan)
                        EnhancedSpanBuilder.basedOn(this.tracer, "AnOperation")
                                .optionallyWithTag("ANumberKey", expectedValue)
                                .start();

        assertThat(builtSpan.tags().entrySet(), hasSize(1));
        assertThat(builtSpan.tags(), hasEntry("ANumberKey", expectedValue));
    }
}
