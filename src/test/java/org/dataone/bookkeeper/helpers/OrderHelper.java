/*
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright 2019
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.dataone.bookkeeper.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.jackson.Jackson;
import org.dataone.bookkeeper.BaseTestCase;
import org.dataone.bookkeeper.api.Order;
import org.dataone.bookkeeper.api.OrderItem;
import org.dataone.bookkeeper.jdbi.mappers.OrderMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

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
        order.setUpdated(new Integer(1559768309));

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


    /**
     * Remove test orders
     * @param orderId
     */
    public static void removeTestOrder(Integer orderId) throws SQLException {
        BaseTestCase.dbi.useHandle(handle ->
            handle.execute("DELETE FROM orders WHERE id = ?", orderId)
        );
    }

    /**
     * Insert a test order
     * @param orderId the order id
     * @return orderId the order id
     */
    public static Integer insertTestOrder(Integer orderId, Integer customerId) {

        Order order = OrderHelper.createTestOrder(
            orderId, customerId, StoreHelper.getRandomId(), StoreHelper.getRandomId());

        try {
            BaseTestCase.dbi.useHandle(handle ->
                handle.execute("INSERT INTO orders (" +
                    "id, " +
                    "object, " +
                    "amount, " +
                    "amountReturned, " +
                    "charge, " +
                    "created, " +
                    "currency, " +
                    "customer, " +
                    "email, " +
                    "items, " +
                    "metadata, " +
                    "status, " +
                    "statusTransitions, " +
                    "updated " +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?::json, to_timestamp(?), " +
                    "?, ?, ?, ?::json, ?::json, ?, ?::json, to_timestamp(?))",
                    order.getId(),
                    order.getObject(),
                    order.getAmount(),
                    order.getAmountReturned(),
                    order.getChargeJSON(),
                    order.getCreated(),
                    order.getCurrency(),
                    order.getCustomer(),
                    order.getEmail(),
                    order.getItemsJSON(),
                    order.getMetadataJSON(),
                    order.getStatus(),
                    order.getStatusTransitionsJSON(),
                    order.getUpdated()
                )
            );
        } catch (IOException e) {
            fail(e);
        }
        return orderId;
    }

    /**
     * Insert a test order
     * @param order the order to insert
     * @return the inserted order
     */
    public static Order insertTestOrder(Order order) {

        try {
            BaseTestCase.dbi.useHandle(handle ->
                handle.execute("INSERT INTO orders (" +
                        "id, " +
                        "object, " +
                        "amount, " +
                        "amountReturned, " +
                        "charge, " +
                        "created, " +
                        "currency, " +
                        "customer, " +
                        "email, " +
                        "items, " +
                        "metadata, " +
                        "status, " +
                        "statusTransitions, " +
                        "updated " +
                        ") VALUES (" +
                        "?, ?, ?, ?, ?::json, to_timestamp(?), " +
                        "?, ?, ?, ?::json, ?::json, ?, ?::json, to_timestamp(?))",
                    order.getId(),
                    order.getObject(),
                    order.getAmount(),
                    order.getAmountReturned(),
                    order.getChargeJSON(),
                    order.getCreated(),
                    order.getCurrency(),
                    order.getCustomer(),
                    order.getEmail(),
                    order.getItemsJSON(),
                    order.getMetadataJSON(),
                    order.getStatus(),
                    order.getStatusTransitionsJSON(),
                    order.getUpdated()
                )
            );
        } catch (IOException e) {
            fail(e);
        }
        return order;
    }

    /**
     * Get the number of orders for a given order id
     * @param orderId the order id
     * @return count the number of orders
     */
    public static Integer getTestOrderCountById(Integer orderId) {
        Integer count = BaseTestCase.dbi.withHandle(handle ->
            handle.createQuery("SELECT count(*) FROM orders WHERE id = :id")
                .bind("id", orderId)
                .mapTo(Integer.class)
                .one()
        );
        return count;
    }

    /**
     * Get an order by id
     * @param orderId the order id
     * @return order the order
     */
    public static Order getTestOrderById(Integer orderId) {
        Order order = BaseTestCase.dbi.withHandle(handle ->
            handle.createQuery("SELECT " +
                "id, " +
                "object, " +
                "amount, " +
                "amountReturned, " +
                "charge, " +
                "date_part('epoch', created)::int AS created, " +
                "currency, " +
                "customer, " +
                "email, " +
                "items, " +
                "metadata, " +
                "status, " +
                "statusTransitions, " +
                "date_part('epoch',updated)::int AS updated " +
                "FROM orders WHERE id = :id")
                .bind("id", orderId)
                .map(new OrderMapper())
                .one()
        );
        return order;
    }
}
