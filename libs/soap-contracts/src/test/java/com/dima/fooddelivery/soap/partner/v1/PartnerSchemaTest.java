package com.dima.fooddelivery.soap.partner.v1;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Контракт проверяется схемой, из которой он сгенерирован: то, что мы отправляем, обязано ей
 * соответствовать, а то, что ей не соответствует, должно отклоняться.
 */
class PartnerSchemaTest {

    private static final URL XSD = PartnerSchemaTest.class.getResource("/soap/partner-v1.xsd");

    @Test
    void shouldShipSchemaInsideTheJar() {
        assertNotNull(XSD, "схема должна лежать в ресурсах под soap/partner-v1.xsd");
    }

    @Test
    void shouldMarshalResponseThatMatchesSchema() throws Exception {
        GetMenuResponse response = new GetMenuResponse();
        MenuCategory category = new MenuCategory();
        category.setId(3);
        category.setName("Супы");
        MenuItem item = new MenuItem();
        item.setId(41);
        item.setName("Борщ");
        item.setPrice(new BigDecimal("350.00"));
        item.setAvailable(true);
        category.getItem().add(item);
        response.getCategory().add(category);

        String xml = marshal(response);

        // Если бы классы расходились со схемой, разбор с валидацией упал бы здесь.
        GetMenuResponse parsed = unmarshal(xml, GetMenuResponse.class);
        assertEquals("Борщ", parsed.getCategory().get(0).getItem().get(0).getName());
        assertEquals(new BigDecimal("350.00"), parsed.getCategory().get(0).getItem().get(0).getPrice());
    }

    @Test
    void shouldCarryOrderWithStatusAndTimestamp() throws Exception {
        Order order = new Order();
        order.setId(85);
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(new BigDecimal("300.00"));
        order.setCreatedAt(DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-10-14T01:02:03Z"));
        GetOrderStatusResponse response = new GetOrderStatusResponse();
        response.setOrder(order);

        String xml = marshal(response);

        assertEquals(OrderStatus.PAID, unmarshal(xml, GetOrderStatusResponse.class).getOrder().getStatus());
    }

    @Test
    void shouldRejectNonPositiveId() {
        String xml = "<GetMenuRequest xmlns=\"urn:fooddelivery:partner:v1\"><restaurantId>0</restaurantId></GetMenuRequest>";

        assertThrows(UnmarshalException.class, () -> unmarshal(xml, GetMenuRequest.class));
    }

    @Test
    void shouldRejectUnknownOrderStatus() {
        String xml = "<GetOrderStatusResponse xmlns=\"urn:fooddelivery:partner:v1\"><order><id>1</id>"
                + "<status>TELEPORTED</status><totalAmount>1.00</totalAmount>"
                + "<createdAt>2026-10-14T01:02:03Z</createdAt></order></GetOrderStatusResponse>";

        assertThrows(UnmarshalException.class, () -> unmarshal(xml, GetOrderStatusResponse.class));
    }

    @Test
    void shouldRejectMoneyWithMoreThanTwoFractionDigits() {
        String xml = "<SetItemAvailabilityResponse xmlns=\"urn:fooddelivery:partner:v1\"><item><id>1</id><name>x</name>"
                + "<price>1.005</price><available>true</available></item></SetItemAvailabilityResponse>";

        assertThrows(UnmarshalException.class, () -> unmarshal(xml, SetItemAvailabilityResponse.class));
    }

    private static Schema schema() throws Exception {
        return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(XSD);
    }

    private static String marshal(Object element) throws Exception {
        Marshaller marshaller = JAXBContext.newInstance(ObjectFactory.class).createMarshaller();
        marshaller.setSchema(schema());
        StringWriter out = new StringWriter();
        marshaller.marshal(element, out);

        return out.toString();
    }

    private static <T> T unmarshal(String xml, Class<T> type) throws Exception {
        Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
        unmarshaller.setSchema(schema());

        return unmarshaller.unmarshal(new StreamSource(new StringReader(xml)), type).getValue();
    }
}
