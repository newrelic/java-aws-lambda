/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class TracingRequestStreamHandlerTest {

    private static final MockTracer mockTracer = new MockTracer();

    @BeforeClass
    public static void beforeClass() {
        GlobalTracerTestUtils.initTracer(mockTracer);
    }

    @Before
    public void before() {
        mockTracer.reset();
        // reset isColdStart before each test
        LambdaTracing.isColdStart.set(true);
    }

    @Test
    public void testSpan() {
        final MyRequestHandler handler = new MyRequestHandler();

        byte[] input1 = { '1', '2', '3' };
        ByteArrayInputStream inputStream1 = new ByteArrayInputStream(input1);
        ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
        handler.handleRequest(inputStream1, outputStream1, createContext());

        final List<MockSpan> mockSpans = mockTracer.finishedSpans();
        final MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("handleRequest", mockSpan.operationName());
        Assert.assertEquals("123", mockSpan.tags().get("aws.requestId"));
        Assert.assertEquals("arn", mockSpan.tags().get("aws.lambda.arn"));
        Assert.assertEquals(true, mockSpan.tags().get("aws.lambda.coldStart"));

        // Clear out span
        mockTracer.reset();

        byte[] input2 = { 'a', 'b', 'c' };
        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(input2);
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        handler.handleRequest(inputStream2, outputStream2, createContext());

        final MockSpan secondSpan = mockTracer.finishedSpans().get(0);
        Assert.assertNull(secondSpan.tags().get("aws.lambda.coldStart"));

        Assert.assertArrayEquals(outputStream1.toByteArray(), new byte[] { '1', '2', '3', '?' });
        Assert.assertArrayEquals(outputStream2.toByteArray(), new byte[] { 'a', 'b', 'c', '?' });
    }

    static class MyRequestHandler implements TracingRequestStreamHandler {

        @Override
        public void doHandleRequest(InputStream input, OutputStream output, Context context) {
            try {
                int in;
                while ((in = input.read()) != -1) {
                    output.write(in);
                }
                output.write('?');
            } catch (IOException e) {
            }
        }
    }

    private Context createContext() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return "123";
            }

            @Override
            public String getLogGroupName() {
                return "logGroupName";
            }

            @Override
            public String getLogStreamName() {
                return "getLogStreamName";
            }

            @Override
            public String getFunctionName() {
                return null;
            }

            @Override
            public String getFunctionVersion() {
                return "LATEST";
            }

            @Override
            public String getInvokedFunctionArn() {
                return "arn";
            }

            @Override
            public CognitoIdentity getIdentity() {
                return new CognitoIdentity() {
                    @Override
                    public String getIdentityId() {
                        return "identity";
                    }

                    @Override
                    public String getIdentityPoolId() {
                        return "identityPoolId";
                    }
                };
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 100;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 510;
            }

            @Override
            public LambdaLogger getLogger() {
                return new LambdaLogger() {
                    @Override
                    public void log(String string) {
                    }

                    @Override
                    public void log(byte[] message) {

                    }
                };
            }
        };
    }
}
