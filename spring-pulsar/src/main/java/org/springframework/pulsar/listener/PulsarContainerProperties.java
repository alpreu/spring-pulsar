/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.pulsar.listener;

import java.time.Duration;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.util.Assert;

/**
 * Contains runtime properties for a listener container.
 *
 * @author Soby Chacko
 */
public class PulsarContainerProperties {

	private static final Duration DEFAULT_CONSUMER_START_TIMEOUT = Duration.ofSeconds(30);

	private Duration consumerStartTimeout = DEFAULT_CONSUMER_START_TIMEOUT;

	/**
	 * Enumeration for ack mode.
	 */
	public enum AckMode {
		/**
		 * Batch ack mode.
		 */
		BATCH,
		/**
		 * Recod ack mode.
		 */
		RECORD,
		/**
		 * Manual ack mode.
		 */
		MANUAL;
	}

	private String[] topics;

	private Pattern topicsPattern;

	private String subscriptionName;

	private SubscriptionType subscriptionType;

	private Schema<?> schema;

	private SchemaType schemaType;

	private Object messageListener;

	private AsyncListenableTaskExecutor consumerTaskExecutor;

	private int maxNumMessages = -1;

	private int maxNumBytes = 10 * 1024 * 1024;

	private int batchTimeout = 100;

	private boolean batchListener;

	private AckMode ackMode = AckMode.BATCH;

	public PulsarContainerProperties(String... topics) {
		this.topics = topics.clone();
		this.topicsPattern = null;
	}

	public PulsarContainerProperties(Pattern topicPattern) {
		this.topicsPattern = topicPattern;
		this.topics = null;
	}

	public Object getMessageListener() {
		return this.messageListener;
	}

	public void setMessageListener(Object messageListener) {
		this.messageListener = messageListener;
	}

	public AsyncListenableTaskExecutor getConsumerTaskExecutor() {
		return this.consumerTaskExecutor;
	}

	public void setConsumerTaskExecutor(AsyncListenableTaskExecutor consumerExecutor) {
		this.consumerTaskExecutor = consumerExecutor;
	}

	public SubscriptionType getSubscriptionType() {
		return this.subscriptionType;
	}

	public void setSubscriptionType(SubscriptionType subscriptionType) {
		this.subscriptionType = subscriptionType;
	}

	public int getMaxNumMessages() {
		return this.maxNumMessages;
	}

	public void setMaxNumMessages(int maxNumMessages) {
		this.maxNumMessages = maxNumMessages;
	}

	public int getMaxNumBytes() {
		return this.maxNumBytes;
	}

	public void setMaxNumBytes(int maxNumBytes) {
		this.maxNumBytes = maxNumBytes;
	}

	public int getBatchTimeout() {
		return this.batchTimeout;
	}

	public void setBatchTimeout(int batchTimeout) {
		this.batchTimeout = batchTimeout;
	}

	public boolean isBatchListener() {
		return this.batchListener;
	}

	public void setBatchListener(boolean batchListener) {
		this.batchListener = batchListener;
	}

	public AckMode getAckMode() {
		return this.ackMode;
	}

	public void setAckMode(AckMode ackMode) {
		this.ackMode = ackMode;
	}

	public Duration getConsumerStartTimeout() {
		return this.consumerStartTimeout;
	}

	/**
	 * Set the timeout to wait for a consumer thread to start before logging
	 * an error. Default 30 seconds.
	 * @param consumerStartTimeout the consumer start timeout.
	 */
	public void setConsumerStartTimeout(Duration consumerStartTimeout) {
		Assert.notNull(consumerStartTimeout, "'consumerStartTimout' cannot be null");
		this.consumerStartTimeout = consumerStartTimeout;
	}

	public Schema<?> getSchema() {
		return this.schema;
	}

	public void setSchema(Schema<?> schema) {
		this.schema = schema;
	}

	public String[] getTopics() {
		return this.topics;
	}

	public void setTopics(String[] topics) {
		this.topics = topics;
	}

	public Pattern getTopicsPattern() {
		return this.topicsPattern;
	}

	public void setTopicsPattern(Pattern topicsPattern) {
		this.topicsPattern = topicsPattern;
	}

	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	public SchemaType getSchemaType() {
		return this.schemaType;
	}

	public void setSchemaType(SchemaType schemaType) {
		this.schemaType = schemaType;
	}
}
