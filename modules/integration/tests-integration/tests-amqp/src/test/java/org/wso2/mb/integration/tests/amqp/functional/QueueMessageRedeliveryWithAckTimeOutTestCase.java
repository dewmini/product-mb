/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.mb.integration.tests.amqp.functional;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.mb.integration.common.clients.AndesClient;
import org.wso2.mb.integration.common.clients.configurations.AndesJMSConsumerClientConfiguration;
import org.wso2.mb.integration.common.clients.configurations.AndesJMSPublisherClientConfiguration;
import org.wso2.mb.integration.common.clients.operations.utils.AndesClientConstants;
import org.wso2.mb.integration.common.clients.operations.utils.AndesClientConfigurationException;
import org.wso2.mb.integration.common.clients.operations.utils.AndesClientUtils;
import org.wso2.mb.integration.common.clients.operations.utils.ExchangeType;
import org.wso2.mb.integration.common.clients.operations.utils.JMSAcknowledgeMode;
import org.wso2.mb.integration.common.utils.backend.MBIntegrationBaseTest;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

/**
 * Test case where client acknowledgement is not given when needed and that messages gets
 * redelivered through dead letter channel.
 */
public class QueueMessageRedeliveryWithAckTimeOutTestCase extends MBIntegrationBaseTest {

    /**
     * Default acknowledge waiting time
     */
    private static final long DEFAULT_ACK_WAIT_TIMEOUT = 10L;

    /**
     * Number of messages to send
     */
    private static final long SEND_COUNT = 1L;

    /**
     * Number of messages expected
     */
    private static final long EXPECTED_COUNT = 10L;

    /**
     * Initializing test case
     * @throws XPathExpressionException
     */
    @BeforeClass
    public void prepare() throws XPathExpressionException {
        super.init(TestUserMode.SUPER_TENANT_USER);
        AndesClientUtils.sleepForInterval(15000);
    }

    /**
     * 1. Subscribe to a single queue using Client Ack
     * 2. This subscriber will wait a long time for messages (defaultAckWaitTimeout*defaultMaxRedeliveryAttempts)
     * 3. Subscriber will never ack for messages
     * 4. Subscriber will receive same message until defaultMaxRedeliveryAttempts breached
     * 5. After that message will be written to dlc
     * 6. No more message should be delivered after written to DLC
     *
     * @throws org.wso2.mb.integration.common.clients.operations.utils.AndesClientConfigurationException
     * @throws JMSException
     * @throws NamingException
     * @throws IOException
     */
    @Test(groups = {"wso2.mb", "queue"})
    public void performQueueMessageRedeliveryWithAckTimeOutTestCase()
            throws AndesClientConfigurationException, JMSException, NamingException, IOException {

        // Setting system property "AndesAckWaitTimeOut" for andes
        System.setProperty("AndesAckWaitTimeOut", Long.toString(DEFAULT_ACK_WAIT_TIMEOUT * 1000L));

        // Creating a consumer client configuration
        AndesJMSConsumerClientConfiguration consumerConfig = new AndesJMSConsumerClientConfiguration(ExchangeType.QUEUE, "redeliveryQueue");
        consumerConfig.setMaximumMessagesToReceived(5L);
        consumerConfig.setAcknowledgeAfterEachMessageCount(200L);
        consumerConfig.setAcknowledgeMode(JMSAcknowledgeMode.CLIENT_ACKNOWLEDGE);

        // Creating a publisher client configuration
        AndesJMSPublisherClientConfiguration publisherConfig = new AndesJMSPublisherClientConfiguration(ExchangeType.QUEUE, "redeliveryQueue");
        publisherConfig.setNumberOfMessagesToSend(SEND_COUNT);

        // Creating clients
        AndesClient consumerClient = new AndesClient(consumerConfig, 3, true);
        consumerClient.startClient();

        AndesClient publisherClient = new AndesClient(publisherConfig, true);
        publisherClient.startClient();

        AndesClientUtils.waitForMessagesAndShutdown(consumerClient, AndesClientConstants.DEFAULT_RUN_TIME * 2L);

        // Evaluating
        Assert.assertEquals(publisherClient.getSentMessageCount(), SEND_COUNT, "Message send failed");
        Assert.assertEquals(consumerClient.getReceivedMessageCount(), EXPECTED_COUNT, "Did not receive expected message count");
    }
}
