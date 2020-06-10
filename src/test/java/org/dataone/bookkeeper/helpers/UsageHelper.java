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

import org.dataone.bookkeeper.BaseTestCase;
import org.dataone.bookkeeper.api.Usage;

import javax.validation.constraints.NotNull;
import java.sql.SQLException;

public class UsageHelper {

    /**
     * Insert a test quota with a given id and subscription id
     * @param usageId
     * @param quotaId
     * @param instanceId
     * @param quantity
     * @param status
     * @return
     */
    public static Integer insertTestUsage(Integer usageId, Integer quotaId, String instanceId, Double quantity, String status)  throws SQLException {
        BaseTestCase.dbi.useHandle(handle ->
                handle.execute(
                        "INSERT INTO usages " +
                                "(id, " +
                                "object, " +
                                "quotaid, " +
                                "instanceid, " +
                                "quantity, " +
                                "status) " +
                                "VALUES " +
                                "(?, ?, ?, ?, ?, ?)",
                        usageId,
                        "usage",
                        quotaId,
                        instanceId,
                        quantity,
                        status)
        );
        return usageId;
    }

    /**
     * Insert a test usage with a given id, quota type, and instanceId
     * @param quotaId
     * @param instanceId
     * @return
     * @throws SQLException
     */
    public static Integer insertTestUsageInstanceId(Integer usageId, Integer quotaId, String instanceId) {
        BaseTestCase.dbi.useHandle(handle ->
                handle.execute("INSERT INTO usages" +
                                "(id, " +
                                "object, " +
                                "quotaid, " +
                                "instanceid, " +
                                "quantity, " +
                                "status) " +
                                "VALUES " +
                                "(?, ?, ?, ?, ?, ?)",
                        usageId,
                        "usage",
                        quotaId,
                        instanceId,
                        1.0,
                        "active")
        );
        return usageId;
    }

    /**
     * Remove a test usage given its id
     * @param usageId
     */
    public static void removeTestUsage(Integer usageId) throws SQLException {

        BaseTestCase.dbi.useHandle(handle ->
                handle.execute("DELETE FROM usages WHERE id = ?", usageId)
        );
    }

    /**
     * Create a test Storage instance given the usageId, quotaId and instanceId
     * @param usageId
     * @param quotaId
     * @return
     */
    public static Usage createTestStorageUsage(@NotNull Integer usageId, Integer quotaId, String instanceId) {
        Usage usage = new Usage();
        usage.setId(usageId);
        usage.setObject("usage");
        usage.setQuotaId(quotaId);
        usage.setInstanceId(instanceId);
        usage.setQuantity(300000.0);
        usage.setStatus("active");
        return usage;
    }


    /**
     * Return the number of usages for the given usage id
     * @param usageId
     * @return
     */
    public static Integer getUsageCountById(Integer usageId) {
        Integer count = BaseTestCase.dbi.withHandle(handle ->
                handle.createQuery("SELECT count(*) FROM usages WHERE id = :id")
                        .bind("id", usageId)
                        .mapTo(Integer.class)
                        .one()
        );
        return count;
    }

    /**
     * Return a usage instance given a usage id
     * @param usageId
     * @return
     */
    public static Usage getUsageById(Integer usageId) {
        Usage usage = BaseTestCase.dbi.withHandle(handle ->
                handle.createQuery("SELECT id, object, quotaid, instanceid, quantity, status " +
                        "FROM usages WHERE id = :id")
                        .bind("id", usageId)
                        .mapToBean(Usage.class)
                        .one()
        );
        return usage;
    }
}