/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.extension.io.sqs.sink;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import io.siddhi.core.exception.ConnectionUnavailableException;
import io.siddhi.core.util.transport.DynamicOptions;
import io.siddhi.core.util.transport.OptionHolder;
import io.siddhi.extension.io.sqs.util.SQSConstants;

/**
 * Message publisher class for SQS.
 */
public class SQSMessagePublisher {
    private AmazonSQS amazonSQS;
    private SQSSinkConfig sqsSinkConfig;
    private OptionHolder optionHolder;
    private boolean isFIFO;

    public SQSMessagePublisher(SQSSinkConfig config, AmazonSQS sqs, OptionHolder optionHolder, boolean isFIFO) {
        this.amazonSQS = sqs;
        this.isFIFO = isFIFO;
        this.sqsSinkConfig = config;
        this.optionHolder = optionHolder;
    }

    private SendMessageRequest generateMessageRequest(String message) {

        SendMessageRequest messageRequest = new SendMessageRequest()
                .withQueueUrl(sqsSinkConfig.getQueueUrl())
                .withMessageBody(message);

        if (sqsSinkConfig.getDelayIntervalTime() != -1) {
            messageRequest.withDelaySeconds(sqsSinkConfig.getDelayIntervalTime());
        }
        return messageRequest;
    }

    public void sendMessageRequest(Object payload, DynamicOptions dynamicOptions)
            throws ConnectionUnavailableException {

        SendMessageRequest messageRequest = generateMessageRequest((String) payload);
        if (isFIFO) {
            messageRequest
                    .withMessageGroupId(
                            optionHolder.validateAndGetOption(SQSConstants.MESSAGE_GROUP_ID_NAME)
                                    .getValue(dynamicOptions));

            if (optionHolder.getDynamicOptionsKeys().contains(SQSConstants.DEDUPLICATION_ID_NAME)) {
                messageRequest
                        .withMessageDeduplicationId(
                                optionHolder.validateAndGetOption(SQSConstants.DEDUPLICATION_ID_NAME)
                                        .getValue(dynamicOptions));
            }
        }

        SendMessageResult messageResult = amazonSQS.sendMessage(messageRequest);

        if (messageResult.getSdkHttpMetadata().getHttpStatusCode() != SQSConstants.HTTP_SUCCESS) {
            throw new ConnectionUnavailableException("Error occurred when trying to send the message, received http" +
                    " status code : " + messageResult.getSdkHttpMetadata().getHttpStatusCode());
        }
    }
}
