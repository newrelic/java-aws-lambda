/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.aws;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.CodeCommitEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;

import java.util.List;
import java.util.Map;

/**
 * Supported event sources in Lambda.
 * https://docs.aws.amazon.com/lambda/latest/dg/invoking-lambda-function.html
 *
 * <p>Note, that list might be incomplete, as it is missing Application Load Balancer.
 * https://docs.aws.amazon.com/elasticloadbalancing/latest/application/lambda-functions.html
 *
 * <p>For how to parse from a map, see sample events published by event source.
 * https://docs.aws.amazon.com/lambda/latest/dg/eventsources.html
 */
final class EventSourceParser {

    private EventSourceParser() {
    }

    static String parseEventSourceArn(Object object) {
        if (object instanceof Map) {
            return parseFromMap((Map) object);
        } else if (object instanceof S3EventNotification) {
            return parseS3BucketArn(object);
        } else if (object instanceof S3Event) {
            return parseS3EventBucketArn(object);
        } else if (object instanceof SNSEvent) {
            return parseSNSEventSubscriptionArn(object);
        } else if (object instanceof SQSEvent) {
            return parseSQSEventSourceArn(object);
        } else if (object instanceof KinesisEvent) {
            return parseKinesisStreamEventSourceArn(object);
        } else if (object instanceof KinesisFirehoseEvent) {
            return parseKinesisFirehoseDeliveryStreamArn(object);
        } else if (object instanceof DynamodbEvent) {
            return parseDynamodbEventSourceArn(object);
        } else if (object instanceof CodeCommitEvent) {
            return parseCodeCommitEventSourceArn(object);
        } else if (object instanceof APIGatewayProxyRequestEvent) {
            return parseAPIGatewayProxyRequestEventUserArn(object);
        } else if (object instanceof APIGatewayV2ProxyRequestEvent) {
            return parseAPIGatewayV2ProxyRequestEventUserArn(object);
        }
        return null;
    }

    private static String parseFromMap(Map input) {
        try {
            if (input.get("streamArn") != null) {
                return (String) input.get("streamArn");
            } else if (input.get("deliveryStreamArn") != null) {
                return (String) input.get("deliveryStreamArn");
            } else if (input.get("requestContext") != null) {
                Map context = (Map) input.get("requestContext");

                // load balancer
                final Map elb = (Map) context.get("elb");
                if (elb != null) {
                    return (String) elb.get("targetGroupArn");
                }

                // api gateway
                final Map identity = (Map) context.get("identity");
                if (identity != null) {
                    return (String) identity.get("userArn");
                }
            } else if (input.get("detail") != null) { // AWS Cloudwatch
                Map detail = (Map) input.get("detail");
                return (String) detail.get("eventSource");
            }

            final List<Object> records = (List<Object>) input.get("Records");
            final Map record = (Map) records.get(0);

            if (record.get("eventSourceARN") != null) {
                return (String) record.get("eventSourceARN");
            } else if (record.get("EventSubscriptionArn") != null) {
                return (String) record.get("EventSubscriptionArn");
            } else if (record.containsKey("s3")) { // AWS S3
                final Map s3Event = (Map) record.get("s3");
                final Map bucket = (Map) s3Event.get("bucket");
                return (String) bucket.get("arn");
            }

            return null;
        } catch (Throwable t) {
        }
        return null;
    }

    private static String parseS3BucketArn(Object object) {
        final S3EventNotification notification = (S3EventNotification) object;

        if (notification.getRecords() == null || notification.getRecords().isEmpty()) {
            return null;
        }

        final S3EventNotificationRecord s3EventNotificationRecord =
                notification.getRecords().get(0);
        if (s3EventNotificationRecord == null || s3EventNotificationRecord.getS3() == null) {
            return null;
        }

        final S3 s3 = s3EventNotificationRecord.getS3();
        if (s3.getBucket() == null) {
            return null;
        }

        return s3.getBucket().getArn();
    }

    private static String parseS3EventBucketArn(Object object) {
        final S3Event notification = (S3Event) object;

        if (notification.getRecords() == null || notification.getRecords().isEmpty()) {
            return null;
        }

        com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord s3EventNotificationRecord =
                notification.getRecords().get(0);

        if (s3EventNotificationRecord == null || s3EventNotificationRecord.getS3() == null) {
            return null;
        }

        final com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity s3 = s3EventNotificationRecord.getS3();
        if (s3.getBucket() == null) {
            return null;
        }

        return s3.getBucket().getArn();
    }

    private static String parseSNSEventSubscriptionArn(Object object) {
        final SNSEvent snsEvent = (SNSEvent) object;

        final List<SNSEvent.SNSRecord> records = snsEvent.getRecords();
        if (records == null || records.isEmpty()) {
            return null;
        }

        final SNSEvent.SNSRecord snsRecord = records.get(0);
        if (snsRecord == null) {
            return null;
        }

        return snsRecord.getEventSubscriptionArn();
    }

    private static String parseSQSEventSourceArn(Object object) {
        final SQSEvent sqsEvent = (SQSEvent) object;

        final List<SQSEvent.SQSMessage> records = sqsEvent.getRecords();
        if (records == null || records.isEmpty()) {
            return null;
        }

        final SQSEvent.SQSMessage sqsMessage = records.get(0);
        if (sqsMessage == null) {
            return null;
        }

        return sqsMessage.getEventSourceArn();
    }

    private static String parseKinesisStreamEventSourceArn(Object object) {
        final KinesisEvent kinesisEvent = (KinesisEvent) object;

        final List<KinesisEvent.KinesisEventRecord> records = kinesisEvent.getRecords();
        if (records == null || records.isEmpty()) {
            return null;
        }

        final KinesisEvent.KinesisEventRecord kinesisEventRecord = records.get(0);
        if (kinesisEventRecord == null) {
            return null;
        }

        return kinesisEventRecord.getEventSourceARN();
    }

    private static String parseKinesisFirehoseDeliveryStreamArn(Object object) {
        final KinesisFirehoseEvent kinesisFirehoseEvent = (KinesisFirehoseEvent) object;

        return kinesisFirehoseEvent.getDeliveryStreamArn();
    }

    private static String parseDynamodbEventSourceArn(Object object) {
        final DynamodbEvent dynamodbEvent = (DynamodbEvent) object;

        final List<DynamodbEvent.DynamodbStreamRecord> records = dynamodbEvent.getRecords();
        if (records == null || records.isEmpty()) {
            return null;
        }

        final DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord = records.get(0);
        if (dynamodbStreamRecord == null) {
            return null;
        }

        return dynamodbStreamRecord.getEventSourceARN();
    }

    private static String parseCodeCommitEventSourceArn(Object object) {
        final CodeCommitEvent codeCommitEvent = (CodeCommitEvent) object;

        final List<CodeCommitEvent.Record> records = codeCommitEvent.getRecords();
        if (records == null || records.isEmpty()) {
            return null;
        }

        final CodeCommitEvent.Record record = records.get(0);
        if (record == null) {
            return null;
        }

        return record.getEventSourceArn();
    }

    private static String parseAPIGatewayProxyRequestEventUserArn(Object object) {
        final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent =
                (APIGatewayProxyRequestEvent) object;

        final APIGatewayProxyRequestEvent.ProxyRequestContext requestContext =
                apiGatewayProxyRequestEvent.getRequestContext();
        if (requestContext == null) {
            return null;
        }

        final APIGatewayProxyRequestEvent.RequestIdentity identity = requestContext.getIdentity();
        if (identity == null) {
            return null;
        }

        return identity.getUserArn();
    }

    private static String parseAPIGatewayV2ProxyRequestEventUserArn(Object object) {
        APIGatewayV2ProxyRequestEvent apiGatewayV2ProxyRequestEvent =
                (APIGatewayV2ProxyRequestEvent) object;

        APIGatewayV2ProxyRequestEvent.RequestContext requestContext =
                apiGatewayV2ProxyRequestEvent.getRequestContext();
        if (requestContext == null) {
            return null;
        }

        APIGatewayV2ProxyRequestEvent.RequestIdentity identity = requestContext.getIdentity();
        if (identity == null) {
            return null;
        }

        return identity.getUserArn();
    }
}
