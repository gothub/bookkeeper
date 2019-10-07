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

package org.dataone.bookkeeper.jdbi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.dataone.bookkeeper.BaseTestCase;
import org.dataone.bookkeeper.api.Customer;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.helpers.CustomerHelper;
import org.dataone.bookkeeper.helpers.StoreHelper;
import org.dataone.bookkeeper.helpers.QuotaHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the Quota data access object
 */
public class QuotaStoreTest extends BaseTestCase {

    // The QuotaStore to test
    private QuotaStore quotaStore;

    // A list of quota ids used in testing
    private List<Integer> quotaIds = new ArrayList<Integer>();

    // A list of customer ids used in testing
    private List<Integer> customerIds = new ArrayList<Integer>();

    /**
     * Set up the Store for testing
     */
    @BeforeEach
    public void init() {
        quotaStore = dbi.onDemand(QuotaStore.class);
    }

    /**
     * Tear down resources
     */
    @AfterEach
    public void tearDown() {
        // Remove test quota entries
        for (Integer quotaId : this.quotaIds) {
            try {
                QuotaHelper.removeTestQuota(quotaId);
            } catch (SQLException e) {
                fail();
            }
        }

        // Remove test customer entries
        for (Integer customerId : this.customerIds) {
            try {
                CustomerHelper.removeTestCustomer(customerId);
            } catch (SQLException e) {
                fail();
            }
        }
    }

    /**
     * Test getting the full Quota list
     */
    @Test
    @DisplayName("Test listing the quotas")
    public void testListQuotas() {
        assertTrue(quotaStore.listQuotas().size() >= 11);
    }

    /**
     * Test getting a single quota by ID
     */
    @Test
    @DisplayName("Test get quota")
    public void testGetQuota() {

        assertTrue(quotaStore.getQuota(1).getId().equals(1));
    }

    /**
     * Test getting quotas by customer ID
     */
    @Test
    @DisplayName("Test getting quotas by customer ID")
    public void testGetQuotasByCustomerId() {

        try {
            Integer customerId = CustomerHelper.insertTestCustomer(StoreHelper.getRandomId());
            this.customerIds.add(customerId); // To be deleted
            Integer quotaId = QuotaHelper.insertTestQuotaWithCustomer(StoreHelper.getRandomId(), customerId);
            this.quotaIds.add(quotaId); // To be deleted
            assertTrue(quotaStore.findQuotasByCustomerId(customerId).size() == 1);
            assertThat(quotaStore.findQuotasByCustomerId(0).isEmpty());

        } catch (SQLException e) {
            fail();
        }
    }

    /**
     * Test getting quotas by subject
     */
    @Test
    @DisplayName("Test getting quotas by subject")
    public void testGetQuotasBySubject() {
        try {
            Customer customer = CustomerHelper.insertTestCustomer(
                CustomerHelper.createCustomer(StoreHelper.getRandomId()));
            this.customerIds.add(customer.getId());
            Integer quotaOneId = QuotaHelper.insertTestQuotaWithSubject(
                StoreHelper.getRandomId(), customer.getSubject());
            Integer quotaTwoId = QuotaHelper.insertTestQuotaWithSubject(
                StoreHelper.getRandomId(), customer.getSubject());
            assertTrue(quotaStore.findQuotasBySubject(customer.getSubject()).size() == 2);
        } catch (SQLException e) {
            fail(e);
        } catch (JsonProcessingException e) {
            fail(e);
        }
    }

    /**
     * Test inserting a Quota instance
     */
    @Test
    @DisplayName("Test inserting a Quota instance")
    public void testInsertWithQuota() {
        try {
            Integer quotaId = StoreHelper.getRandomId();
            Integer customerId = null;
            Quota quota = QuotaHelper.createTestStorageQuota(quotaId, customerId);
            this.quotaIds.add(quotaId);
            quotaStore.insert(quota);
            assertThat(QuotaHelper.getQuotaCountById(quotaId) == 1);
        } catch (Exception e) {
            fail();
        }

    }

    /**
     * Test updating a quota
     */
    @Test
    @DisplayName("Test updating a quota")
    public void testUpdate() {
        try {
            Integer customerId = CustomerHelper.insertTestCustomer(StoreHelper.getRandomId());
            this.customerIds.add(customerId); // Clean up
            Integer quotaId = QuotaHelper.insertTestQuotaWithCustomer(StoreHelper.getRandomId(), customerId);
            this.quotaIds.add(quotaId); // Clean up
            Quota quota = new Quota();
            quota.setId(quotaId);
            quota.setObject("quota");
            String quotaName = "test_quota_" + StoreHelper.getRandomId().toString();
            quota.setName(quotaName);
            quota.setSoftLimit(56789);
            quota.setHardLimit(567890);
            quota.setUsage(96789);
            quota.setUnit("megabyte");
            quota.setCustomerId(customerId);
            quotaStore.update(quota);
            assertThat(QuotaHelper.getQuotaById(quotaId).getName() == quotaName);
            assertThat(QuotaHelper.getQuotaById(quotaId).getSoftLimit() == 56789);
            assertThat(QuotaHelper.getQuotaById(quotaId).getHardLimit() == 567890);
            assertThat(QuotaHelper.getQuotaById(quotaId).getUsage() == 96789);
        } catch (SQLException e) {
            fail();
        }
    }

    /**
     * Test deleting a quota
     */
    @Test
    @DisplayName("Test deleting a quota")
    public void testDelete() {
        Integer customerId;
        Integer quotaId = null;
        try {
            customerId = CustomerHelper.insertTestCustomer(StoreHelper.getRandomId());
            this.customerIds.add(customerId); // Clean up
             quotaId = QuotaHelper.insertTestQuotaWithCustomer(StoreHelper.getRandomId(), customerId);
            quotaStore.delete(quotaId);
            assertThat(QuotaHelper.getQuotaCountById(quotaId) == 0);
        } catch (SQLException e) {
            this.quotaIds.add(quotaId); // Clean up on fail
            fail();
        }

    }
}