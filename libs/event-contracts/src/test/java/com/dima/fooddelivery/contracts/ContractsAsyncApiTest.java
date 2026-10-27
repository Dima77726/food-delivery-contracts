package com.dima.fooddelivery.contracts;

import com.dima.fooddelivery.contracts.audit.AuditTopics;
import com.dima.fooddelivery.contracts.order.OrderEventTopics;
import com.dima.fooddelivery.contracts.payment.PaymentTopics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AsyncAPI-документ не должен разойтись с кодом: каждый топик Kafka из констант контракта обязан быть
 * в нём каналом. Без YAML-парсера — у модуля нет зависимостей, и тест их не добавляет: достаточно
 * найти строку {@code address: <топик>}. Полную проверку схемы делает CI ({@code asyncapi validate}).
 */
class ContractsAsyncApiTest {

    private static final Path SPEC = Path.of("..", "..", "asyncapi", "food-delivery.asyncapi.yaml");

    @Test
    void shouldDescribeEveryKafkaTopicOfTheContracts() throws IOException {
        String spec = Files.readString(SPEC);

        for (String topic : List.of(OrderEventTopics.ORDER_EVENTS, PaymentTopics.PAYMENT_COMMANDS,
                PaymentTopics.PAYMENT_EVENTS, AuditTopics.AUDIT_EVENTS)) {
            assertTrue(spec.contains("address: " + topic + "\n") || spec.contains("address: " + topic + "\r\n"),
                    "в AsyncAPI нет канала для топика " + topic);
        }
    }

    @Test
    void shouldReferenceOnlyDeclaredChannelsFromOperations() throws IOException {
        String spec = Files.readString(SPEC);
        Matcher refs = Pattern.compile("\\$ref: '#/channels/([A-Za-z]+)'").matcher(spec);
        while (refs.find()) {
            String channel = refs.group(1);
            assertTrue(Pattern.compile("(?m)^  " + channel + ":\\s*$").matcher(spec).find(),
                    "операция ссылается на необъявленный канал " + channel);
        }
    }
}
