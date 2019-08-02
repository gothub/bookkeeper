package org.dataone.bookkeeper.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.jackson.Jackson;
import org.dataone.bookkeeper.api.Order;
import org.dataone.bookkeeper.api.OrderItem;

import java.util.LinkedList;
import java.util.List;

/**
 * A delegate class with helper methods for manipulating the orders table during testing
 */
public class OrderHelper {

    /**
     * Create a test order
     * @param orderId
     * @return
     */
    public static Order createTestOrder(Integer orderId, Integer customerId,
                                        Integer chargeId, Integer invoiceId) {
        Order order = new Order();
        order.setId(orderId);
        order.setObject("order");
        order.setAmount(new Integer(50000));
        order.setAmountReturned(0);
        order.setCharge(OrderHelper.createTestCharge(chargeId, customerId, invoiceId, orderId));
        order.setCreated(new Integer(1559768309));
        order.setCurrency("USD");
        order.setCustomer(customerId);
        order.setEmail("cjones@nceas.ucsb.edu");
        order.setItems(OrderHelper.createTestOrderItems());
        order.setMetadata(Jackson.newObjectMapper().createObjectNode());
        order.setStatus("paid");
        order.setStatusTransitions(OrderHelper.createTestStatusTransitions());
        order.setUpdated(new Integer(1559768309)); // updated

        return order;
    }
    /**
     * Create a test charge object to represent the transaction for the order
     * @param chargeId
     * @param customerId
     * @param invoiceId
     * @param orderId
     * @return charge the charge associated with the order, invoice, and customer
     */
    public static ObjectNode createTestCharge(Integer chargeId, Integer customerId,
                                               Integer invoiceId, Integer orderId) {
        ObjectMapper mapper = Jackson.newObjectMapper();
        ObjectNode charge = mapper.createObjectNode();
        charge.put("id", chargeId);
        charge.put("object", "charge");
        charge.put("amount", new Integer(50000).intValue());
        charge.put("amountRefunded", new Integer(0).intValue());
        charge.put("created", 1559768309);
        charge.put("currency", "USD");
        charge.put("customer", customerId);
        charge.put("description", "DataONE Order # 1");
        charge.put("invoice", invoiceId);
        charge.set("metadata", mapper.createObjectNode());
        charge.put("order", orderId);
        charge.put("paid", true);
        charge.put("statementDescriptor", "DataONE Order # 1");
        charge.put("status", "succeeded");

        return charge;
    }


    /**
     * Create a test order item list
     * @return orderItems the list of order items for the order
     */
    public static List<OrderItem> createTestOrderItems() {
        List<OrderItem> orderItems = new LinkedList<OrderItem>();
        OrderItem orderItem = new OrderItem();
        orderItem.setObject("order_item");
        orderItem.setAmount(50000);
        orderItem.setCurrency("USD");
        orderItem.setDescription("DataONE Individual Membership");
        orderItem.setParent(new Integer(1000));
        orderItem.setQuantity(new Integer(1));
        orderItem.setType("sku");

        orderItems.add(orderItem);
        return orderItems;
    }


    /**
     * Create a test status transitions object for an order
     * @return
     */
    public static ObjectNode createTestStatusTransitions() {
        ObjectMapper mapper = Jackson.newObjectMapper();
        ObjectNode statusTransitions = mapper.createObjectNode();
        statusTransitions.put("created", 1559768309);
        statusTransitions.put("paid", 1559768309);

        return statusTransitions;
    }


}