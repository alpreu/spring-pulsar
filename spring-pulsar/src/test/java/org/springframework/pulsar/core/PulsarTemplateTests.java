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

package org.springframework.pulsar.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.MessageRouter;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TopicMetadata;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@code PulsarTemplate}.
 *
 * @author Soby Chacko
 * @author Chris Bono
 */
class PulsarTemplateTests extends AbstractContainerBaseTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("sendMessageTestProvider")
	void sendMessageTest(String topic, Map<String, Object> producerConfig, SendHandler<Object> handler, MessageRouter router) throws Exception {
		String subscription = topic + "-sub";
		String msgPayload = topic + "-msg";
		if (router != null) {
			try (PulsarAdmin admin = PulsarAdmin.builder().serviceHttpUrl(getHttpServiceUrl()).build()) {
				admin.topics().createPartitionedTopic("persistent://public/default/" + topic, 1);
			}
		}
		try (PulsarClient client = PulsarClient.builder().serviceUrl(getPulsarBrokerUrl()).build()) {
			try (Consumer<String> consumer = client.newConsumer(Schema.STRING).topic(topic).subscriptionName(subscription).subscribe()) {
				PulsarProducerFactory<String> producerFactory = new DefaultPulsarProducerFactory<>(client, producerConfig);
				PulsarTemplate<String> pulsarTemplate = new PulsarTemplate<>(producerFactory);

				Object sendResponse = handler.doSend(pulsarTemplate, topic, msgPayload, router);
				if (sendResponse instanceof CompletableFuture) {
					sendResponse = ((CompletableFuture<?>) sendResponse).get(3, TimeUnit.SECONDS);
				}
				assertThat(sendResponse).isNotNull();

				CompletableFuture<Message<String>> receiveMsgFuture = consumer.receiveAsync();
				Message<String> msg = receiveMsgFuture.get(3, TimeUnit.SECONDS);
				assertThat(msg.getData()).asString().isEqualTo(msgPayload);

				// Make sure the producer was closed by the template (albeit indirectly as client removes closed producers)
				await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
						assertThat(client).extracting("producers").asInstanceOf(InstanceOfAssertFactories.COLLECTION).isEmpty());
			}
		}
	}

	static Stream<Arguments> sendMessageTestProvider() {
		return Stream.of(
				arguments(Named.of("sendMessageToDefaultTopicNoRouter", "smt-topic-1"), Collections.singletonMap("topicName", "smt-topic-1"),
						(SendHandler<MessageId>) (template, topic, msg, router) -> template.send(msg),
						null),
				arguments(Named.of("sendMessageToDefaultTopicWithRouter", "smt-topic-2"), Collections.singletonMap("topicName", "smt-topic-2"),
						(SendHandler<MessageId>) (template, topic, msg, router) -> template.send(msg, router),
						mockRouter()),
				arguments(Named.of("sendMessageToSpecificTopicNoRouter", "smt-topic-3"), Collections.emptyMap(),
						(SendHandler<MessageId>) (template, topic, msg, router) -> template.send(topic, msg),
						null),
				arguments(Named.of("sendMessageToSpecificTopicWithRouter", "smt-topic-4"), Collections.emptyMap(),
						(SendHandler<MessageId>) PulsarTemplate::send,
						mockRouter()),
				arguments(Named.of("sendAsyncMessageToDefaultTopicNoRouter", "smt-topic-5"), Collections.singletonMap("topicName", "smt-topic-5"),
						(SendHandler<CompletableFuture<MessageId>>) (template, topic, msg, router) -> template.sendAsync(msg),
						null),
				arguments(Named.of("sendAsyncMessageToDefaultTopicWithRouter", "smt-topic-6"), Collections.singletonMap("topicName", "smt-topic-6"),
						(SendHandler<CompletableFuture<MessageId>>) (template, topic, msg, router) -> template.sendAsync(msg, router),
						mockRouter()),
				arguments(Named.of("sendAsyncMessageToSpecificTopicNoRouter", "smt-topic-7"), Collections.emptyMap(),
						(SendHandler<CompletableFuture<MessageId>>) (template, topic, msg, router) -> template.sendAsync(topic, msg),
						null),
				arguments(Named.of("sendAsyncMessageToSpecificTopicWithRouter", "smt-topic-8"), Collections.emptyMap(),
						(SendHandler<CompletableFuture<MessageId>>) PulsarTemplate::sendAsync,
						mockRouter())
		);
	}

	private static MessageRouter mockRouter() {
		MessageRouter router = mock(MessageRouter.class);
		when(router.choosePartition(any(Message.class), any(TopicMetadata.class))).thenReturn(0);
		return router;
	}

	@FunctionalInterface
	interface SendHandler<V>  {
		V doSend(PulsarTemplate<String> template, String topic, String msg, MessageRouter router) throws PulsarClientException;
	}
}
