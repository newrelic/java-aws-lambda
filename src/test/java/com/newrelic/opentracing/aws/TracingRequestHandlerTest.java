/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.CloudFrontEvent;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.CodeCommitEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TracingRequestHandlerTest {

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
        handler.handleRequest("world", createContext());

        final List<MockSpan> mockSpans = mockTracer.finishedSpans();
        final MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("handleRequest", mockSpan.operationName());
        Assert.assertEquals("123", mockSpan.tags().get("aws.requestId"));
        Assert.assertEquals("arn", mockSpan.tags().get("aws.lambda.arn"));
        Assert.assertEquals(true, mockSpan.tags().get("aws.lambda.coldStart"));

        // Clear out span
        mockTracer.reset();

        handler.handleRequest("world", createContext());
        final MockSpan secondSpan = mockTracer.finishedSpans().get(0);
        Assert.assertNull(secondSpan.tags().get("aws.lambda.coldStart"));
    }

    @Test
    public void testError() {
        Error error = null;

        final ErrorRequestHandler handler = new ErrorRequestHandler();
        try {
            handler.handleRequest("abcdefg", createContext());
        } catch (Error e) {
            error = e;
        }

        // Make sure TraceRequestHandler rethrows error
        Assert.assertNotNull(error);

        final List<MockSpan> mockSpans = mockTracer.finishedSpans();
        final MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("handleRequest", mockSpan.operationName());
        Assert.assertEquals("123", mockSpan.tags().get("aws.requestId"));
        Assert.assertEquals("arn", mockSpan.tags().get("aws.lambda.arn"));
        Assert.assertEquals(true, mockSpan.tags().get("aws.lambda.coldStart"));

        Assert.assertEquals(1, mockSpan.logEntries().size());
        final MockSpan.LogEntry logEntry = mockSpan.logEntries().get(0);
        final Map<String, ?> errorFields = logEntry.fields();
        Assert.assertNotNull(errorFields.get("stack"));
        Assert.assertEquals("Exception", errorFields.get("error.kind").toString());
        Assert.assertEquals("java.lang.Error: abcdefg", errorFields.get("error.object").toString());
        Assert.assertEquals("error", errorFields.get("event"));
        Assert.assertEquals("abcdefg", errorFields.get("message"));
    }

    @Test
    public void testS3Event() {
        final MyS3RequestHandler myS3RequestHandler = new MyS3RequestHandler();

        final List<S3EventNotification.S3EventNotificationRecord> records = new ArrayList<>();
        final S3EventNotification.UserIdentityEntity userIdentity =
                new S3EventNotification.UserIdentityEntity("principalId");
        final S3EventNotification.S3BucketEntity s3BucketEntity =
                new S3EventNotification.S3BucketEntity("bucketName", userIdentity, "s3ARN");
        final S3EventNotification.S3Entity s3Entity =
                new S3EventNotification.S3Entity("s3Entity", s3BucketEntity, null, null);
        final S3EventNotification.S3EventNotificationRecord record =
                new S3EventNotification.S3EventNotificationRecord(
                        "awsRegion",
                        "eventName",
                        "eventSource",
                        "2010-06-30T01:20+02:00",
                        "eventVersion",
                        null,
                        null,
                        s3Entity,
                        null);
        records.add(record);

        myS3RequestHandler.handleRequest(new S3Event(records), createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals("s3ARN", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testSNSEvent() {
        final MySNSRequestHandler mySNSRequestHandler = new MySNSRequestHandler();

        final SNSEvent snsEvent = new SNSEvent();
        final SNSEvent.SNSRecord snsRecord = new SNSEvent.SNSRecord();
        snsRecord.setEventSubscriptionArn("SNSEventSubscriptionArn");
        final SNSEvent.SNS sns = new SNSEvent.SNS();
        final SNSEvent.MessageAttribute messageAttribute = new SNSEvent.MessageAttribute();
        final Map<String, SNSEvent.MessageAttribute> messageAttributes = new HashMap<>();
        messageAttributes.put("messageAttributes.key", messageAttribute);
        sns.setMessageAttributes(messageAttributes);
        snsRecord.setSns(sns);
        final List<SNSEvent.SNSRecord> records = new ArrayList<>();
        records.add(snsRecord);
        snsEvent.setRecords(records);

        mySNSRequestHandler.handleRequest(snsEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals("SNSEventSubscriptionArn", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testSQSEvent() {
        final MySQSRequestHandler mySQSRequestHandler = new MySQSRequestHandler();

        final SQSEvent sqsEvent = new SQSEvent();
        final SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setEventSourceArn("SQSEventSourceArn");
        final List<SQSEvent.SQSMessage> records = new ArrayList<>();
        records.add(sqsMessage);
        sqsEvent.setRecords(records);

        mySQSRequestHandler.handleRequest(sqsEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals("SQSEventSourceArn", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testKinesisStreamEvent() {
        final MyKinesisStreamRequestHandler myKinesisStreamRequestHandler =
                new MyKinesisStreamRequestHandler();

        final KinesisEvent kinesisStreamEvent = new KinesisEvent();
        final KinesisEvent.KinesisEventRecord kinesisEventRecord =
                new KinesisEvent.KinesisEventRecord();
        kinesisEventRecord.setEventSourceARN("KinesisStreamEventSourceARN");
        final List<KinesisEvent.KinesisEventRecord> records = new ArrayList<>();
        records.add(kinesisEventRecord);
        kinesisStreamEvent.setRecords(records);

        myKinesisStreamRequestHandler.handleRequest(kinesisStreamEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "KinesisStreamEventSourceARN", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testKinesisFirehoseEvent() {
        final MyKinesisFirehoseRequestHandler myKinesisFirehoseRequestHandler =
                new MyKinesisFirehoseRequestHandler();

        final KinesisFirehoseEvent kinesisFirehoseEvent = new KinesisFirehoseEvent();
        kinesisFirehoseEvent.setDeliveryStreamArn("KinesisFirehoseDeliveryStreamArn");

        myKinesisFirehoseRequestHandler.handleRequest(kinesisFirehoseEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "KinesisFirehoseDeliveryStreamArn", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testDynamoDBEvent() {
        final MyDynamoDBRequestHandler myDynamoDBRequestHandler = new MyDynamoDBRequestHandler();

        final DynamodbEvent dynamodbEvent = new DynamodbEvent();
        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord =
                new DynamodbEvent.DynamodbStreamRecord();
        dynamodbStreamRecord.setEventSourceARN("DynamodbEventSourceARN");
        List<DynamodbEvent.DynamodbStreamRecord> records = new ArrayList<>();
        records.add(dynamodbStreamRecord);
        dynamodbEvent.setRecords(records);

        myDynamoDBRequestHandler.handleRequest(dynamodbEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals("DynamodbEventSourceARN", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testCodeCommitEvent() {
        final MyCodeCommitRequestHandler myCodeCommitRequestHandler = new MyCodeCommitRequestHandler();

        final CodeCommitEvent codeCommitEvent = new CodeCommitEvent();
        final CodeCommitEvent.Record record = new CodeCommitEvent.Record();
        record.setEventSourceArn("CodeCommitEventSourceARN");
        List<CodeCommitEvent.Record> records = new ArrayList<>();
        records.add(record);
        codeCommitEvent.setRecords(records);

        myCodeCommitRequestHandler.handleRequest(codeCommitEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals("CodeCommitEventSourceARN", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testAPIGatewayProxyRequestEvent() {
        final MyApiGatewayProxyRequestHandler myApiGatewayProxyRequestHandler =
                new MyApiGatewayProxyRequestHandler();

        final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent =
                new APIGatewayProxyRequestEvent();
        final APIGatewayProxyRequestEvent.RequestIdentity requestIdentity =
                new APIGatewayProxyRequestEvent.RequestIdentity();
        requestIdentity.setUserArn("APIGatewayProxyRequestEventUserARN");
        final APIGatewayProxyRequestEvent.ProxyRequestContext proxyRequestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();
        proxyRequestContext.setIdentity(requestIdentity);
        apiGatewayProxyRequestEvent.setRequestContext(proxyRequestContext);
        apiGatewayProxyRequestEvent.setHeaders(new HashMap<>());

        myApiGatewayProxyRequestHandler.handleRequest(apiGatewayProxyRequestEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "APIGatewayProxyRequestEventUserARN", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    public void testAPIGatewayProxyRequestResponseEvent() {
        int expectedStatusCode = 200;
        final MyApiGatewayProxyRequestResponseHandler myApiGatewayProxyRequestHandler =
                new MyApiGatewayProxyRequestResponseHandler(expectedStatusCode);

        final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent =
                new APIGatewayProxyRequestEvent();
        final APIGatewayProxyRequestEvent.RequestIdentity requestIdentity =
                new APIGatewayProxyRequestEvent.RequestIdentity();
        requestIdentity.setUserArn("APIGatewayProxyRequestEventUserARN");
        final APIGatewayProxyRequestEvent.ProxyRequestContext proxyRequestContext =
                new APIGatewayProxyRequestEvent.ProxyRequestContext();

        apiGatewayProxyRequestEvent.setHeaders(new HashMap<>());
        proxyRequestContext.setIdentity(requestIdentity);
        apiGatewayProxyRequestEvent.setRequestContext(proxyRequestContext);

        myApiGatewayProxyRequestHandler.handleRequest(apiGatewayProxyRequestEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "APIGatewayProxyRequestEventUserARN", span.tags().get("aws.lambda.eventSource.arn"));
        Assert.assertEquals(Integer.toString(expectedStatusCode), span.tags().get("http.status_code"));
    }

    @Test
    public void testAPIGatewayV2ProxyRequestEvent() {
        final MyApiGatewayV2ProxyRequestHandler myApiGatewayV2ProxyRequestHandler =
                new MyApiGatewayV2ProxyRequestHandler();

        final APIGatewayV2ProxyRequestEvent apiGatewayV2ProxyRequestEvent =
                new APIGatewayV2ProxyRequestEvent();
        final APIGatewayV2ProxyRequestEvent.RequestIdentity requestIdentity =
                new APIGatewayV2ProxyRequestEvent.RequestIdentity();
        requestIdentity.setUserArn("APIGatewayV2ProxyRequestEventUserARN");
        final APIGatewayV2ProxyRequestEvent.RequestContext proxyRequestContext =
                new APIGatewayV2ProxyRequestEvent.RequestContext();
        proxyRequestContext.setIdentity(requestIdentity);
        apiGatewayV2ProxyRequestEvent.setRequestContext(proxyRequestContext);

        myApiGatewayV2ProxyRequestHandler.handleRequest(apiGatewayV2ProxyRequestEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "APIGatewayV2ProxyRequestEventUserARN", span.tags().get("aws.lambda.eventSource.arn"));
        Assert.assertFalse(span.tags().containsKey("http.status_code"));
    }

    @Test
    public void testAPIGatewayV2ProxyRequestResponseEvent() {
        int expectedStatusCode = 200;
        final MyApiGatewayV2ProxyRequestResponseHandler myApiGatewayV2ProxyRequestHandler =
                new MyApiGatewayV2ProxyRequestResponseHandler(expectedStatusCode);

        final APIGatewayV2ProxyRequestEvent apiGatewayV2ProxyRequestEvent =
                new APIGatewayV2ProxyRequestEvent();
        final APIGatewayV2ProxyRequestEvent.RequestIdentity requestIdentity =
                new APIGatewayV2ProxyRequestEvent.RequestIdentity();
        requestIdentity.setUserArn("APIGatewayV2ProxyRequestEventUserARN");
        final APIGatewayV2ProxyRequestEvent.RequestContext proxyRequestContext =
                new APIGatewayV2ProxyRequestEvent.RequestContext();
        proxyRequestContext.setIdentity(requestIdentity);
        apiGatewayV2ProxyRequestEvent.setRequestContext(proxyRequestContext);

        myApiGatewayV2ProxyRequestHandler.handleRequest(apiGatewayV2ProxyRequestEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "APIGatewayV2ProxyRequestEventUserARN", span.tags().get("aws.lambda.eventSource.arn"));
        Assert.assertEquals(Integer.toString(expectedStatusCode), span.tags().get("http.status_code"));
    }

    @Test
    @Ignore("We would like this but there doesn't seem to be an available arn currently")
    public void testCloudWatchLogsEvent() {
        final MyCloudWatchRequestHandler myCloudWatchRequestHandler = new MyCloudWatchRequestHandler();

        final CloudWatchLogsEvent cloudWatchLogsEvent = new CloudWatchLogsEvent();

        myCloudWatchRequestHandler.handleRequest(cloudWatchLogsEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "CloudWatchLogsEventSourceARN", span.tags().get("aws.lambda.eventSource.arn"));
    }

    @Test
    @Ignore("We would like this but there doesn't seem to be an available arn currently")
    public void testCloudFrontEvent() {
        final MyCloudFrontRequestHandler myCloudFrontRequestHandler = new MyCloudFrontRequestHandler();

        final CloudFrontEvent cloudFrontEvent = new CloudFrontEvent();
        final CloudFrontEvent.Record record = new CloudFrontEvent.Record();

        myCloudFrontRequestHandler.handleRequest(cloudFrontEvent, createContext());
        final MockSpan span = mockTracer.finishedSpans().get(0);
        Assert.assertEquals(
                "CloudFrontEventEventSourceARN", span.tags().get("aws.lambda.eventSource.arn"));
    }

    static class MyRequestHandler implements TracingRequestHandler<String, String> {

        @Override
        public String doHandleRequest(String s, Context context) {
            return "Request Handler says: hello " + s;
        }
    }

    static class ErrorRequestHandler implements TracingRequestHandler<String, String> {

        @Override
        public String doHandleRequest(String s, Context context) {
            throw new Error(s);
        }
    }

    // S3
    static class MyS3RequestHandler implements TracingRequestHandler<S3Event, Object> {

        @Override
        public Object doHandleRequest(S3Event s3Event, Context context) {
            return "null";
        }
    }

    // Simple Notification Service (SNS)
    static class MySNSRequestHandler implements TracingRequestHandler<SNSEvent, Object> {

        @Override
        public Object doHandleRequest(SNSEvent snsEvent, Context context) {
            return "null";
        }
    }

    // Kinesis Stream
    static class MyKinesisStreamRequestHandler
            implements TracingRequestHandler<KinesisEvent, Object> {

        @Override
        public Object doHandleRequest(KinesisEvent kinesisEvent, Context context) {
            return "null";
        }
    }

    // Kinesis Firehose
    static class MyKinesisFirehoseRequestHandler
            implements TracingRequestHandler<KinesisFirehoseEvent, Object> {

        @Override
        public Object doHandleRequest(KinesisFirehoseEvent kinesisFirehoseEvent, Context context) {
            return "null";
        }
    }

    // Dynamo DB
    static class MyDynamoDBRequestHandler implements TracingRequestHandler<DynamodbEvent, Object> {

        @Override
        public Object doHandleRequest(DynamodbEvent dynamodbEvent, Context context) {
            return "null";
        }
    }

    // Simple Queue Service (SQS)
    static class MySQSRequestHandler implements TracingRequestHandler<SQSEvent, Object> {

        @Override
        public Object doHandleRequest(SQSEvent sqsEvent, Context context) {
            return "null";
        }
    }

    // Code Commit
    static class MyCodeCommitRequestHandler
            implements TracingRequestHandler<CodeCommitEvent, Object> {

        @Override
        public Object doHandleRequest(CodeCommitEvent codeCommitEvent, Context context) {
            return "null";
        }
    }

    // API Gateway Proxy Request Event
    static class MyApiGatewayProxyRequestHandler
            implements TracingRequestHandler<APIGatewayProxyRequestEvent, Object> {

        @Override
        public Object doHandleRequest(
                APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
            return "null";
        }
    }

    static class MyApiGatewayProxyRequestResponseHandler
            implements TracingRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

        private int statusCode;

        public MyApiGatewayProxyRequestResponseHandler(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public APIGatewayProxyResponseEvent doHandleRequest(
                APIGatewayProxyRequestEvent apiGatewayV2ProxyRequestEvent, Context context) {
            APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
            responseEvent.setStatusCode(statusCode);
            responseEvent.setBody("null");
            return responseEvent;
        }
    }

    static class MyApiGatewayV2ProxyRequestHandler
            implements TracingRequestHandler<APIGatewayV2ProxyRequestEvent, Object> {

        @Override
        public Object doHandleRequest(
                APIGatewayV2ProxyRequestEvent apiGatewayV2ProxyRequestEvent, Context context) {
            return "null";
        }
    }

    static class MyApiGatewayV2ProxyRequestResponseHandler
            implements TracingRequestHandler<
            APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {

        private int statusCode;

        public MyApiGatewayV2ProxyRequestResponseHandler(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public APIGatewayV2ProxyResponseEvent doHandleRequest(
                APIGatewayV2ProxyRequestEvent apiGatewayV2ProxyRequestEvent, Context context) {
            APIGatewayV2ProxyResponseEvent responseEvent = new APIGatewayV2ProxyResponseEvent();
            responseEvent.setStatusCode(statusCode);
            responseEvent.setBody("null");
            return responseEvent;
        }
    }

    // Cloud Front
    static class MyCloudFrontRequestHandler
            implements TracingRequestHandler<CloudFrontEvent, Object> {

        @Override
        public Object doHandleRequest(CloudFrontEvent cloudFrontEvent, Context context) {
            return "null";
        }
    }

    // Cloud Watch
    static class MyCloudWatchRequestHandler
            implements TracingRequestHandler<CloudWatchLogsEvent, Object> {

        @Override
        public Object doHandleRequest(CloudWatchLogsEvent cloudWatchLogsEvent, Context context) {
            return "null";
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
