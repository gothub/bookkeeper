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

package org.dataone.bookkeeper.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.bookkeeper.api.Customer;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.api.QuotaList;
import org.dataone.bookkeeper.jdbi.QuotaStore;
import org.dataone.bookkeeper.security.DataONEAuthHelper;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

/**
 * The entry point to the quotas collection
 */
@Timed
@Path("/quotas")
@Produces(MediaType.APPLICATION_JSON)
public class QuotasResource extends BaseResource {

    /* The logging facility for this class */
    private Log log = LogFactory.getLog(QuotasResource.class);

    /* The quota store for database calls */
    private final QuotaStore quotaStore;

    /* An instance of the DataONE authn and authz delegate */
    private final DataONEAuthHelper dataONEAuthHelper;

    /**
     * Construct a quota collection
     * @param database  the jdbi database access reference
     */
    public QuotasResource(Jdbi database, DataONEAuthHelper dataONEAuthHelper) {
        this.quotaStore = database.onDemand(QuotaStore.class);
        this.dataONEAuthHelper = dataONEAuthHelper;

    }

    /**
     * List quotas, optionally by subscriptionId or subject.
     * Use start and count to get paginated results
     * @param start  the paging start index
     * @param count  the paging size count
     * @param subscriptionId  the subscriptionId
     * @param subjects  the quota subjects (repeatable and treated as a list)
     * @return quotas  the quota list
     */
    @Timed
    @GET
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public QuotaList listQuotas(
        @Context SecurityContext context,
        @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("count") @DefaultValue("1000") Integer count,
        @QueryParam("subscriptionId") Integer subscriptionId,
        @QueryParam("subject") List<String> subjects) throws WebApplicationException {

        // The calling user injected in the security context via authentication
        Customer caller = (Customer) context.getUserPrincipal();
        boolean isAdmin = this.dataONEAuthHelper.isAdmin(caller.getSubject());

        List<Quota> quotas = new ArrayList<Quota>();
        try {
            if ( subjects != null && subjects.size() > 0 ) {
                // Filter out non-associated subjects if not an admin
                if ( ! isAdmin ) {
                    List<String> associatedSubjects =
                        this.dataONEAuthHelper.getAssociatedSubjects(caller, subjects);
                    if ( associatedSubjects.size() > 0 ) {
                        subjects.addAll(associatedSubjects);
                    }
                }
                // Get quotas if the subject list is non-zero
                if ( subjects.size() > 0 ) {
                    quotas = quotaStore.findQuotasBySubjects(subjects);
                }
            } else if (subscriptionId != null) {

                quotas = quotaStore.findQuotasBySubscriptionId(subscriptionId);

                // Allow admin access only for now
                if ( ! isAdmin ) {
                    throw new Exception(caller.getSubject() + " doesn't have access to subscriptions.");
                }
            } else if ( isAdmin ) {
                // For admins, list all quotas
                quotas = quotaStore.listQuotas();
            } else {
                // Fall back to list product quotas not assigned to users
                quotas = quotaStore.listUnassignedQuotas();
            }
        } catch (Exception e) {
            String message = "Couldn't list quotas: " + e.getMessage();
            throw new WebApplicationException(message, Response.Status.EXPECTATION_FAILED);
        }

        // TODO: Incorporate paging params - new QuotaList(start, count, total, quotas)
        return new QuotaList(quotas);
    }

    /**
     * Create the given quota
     * @param quota  the quota to create
     * @return quota  the created quota
     */
    @Timed
    @POST
    @RolesAllowed("CN=urn:node:CN,DC=dataone,DC=org")
    @Consumes(MediaType.APPLICATION_JSON)
    public Quota create(
        @Context SecurityContext context,
        @NotNull @Valid Quota quota) throws WebApplicationException {
        // Insert the quota after it is validated
        try {
            Integer id = quotaStore.insert(quota);
            quota = quotaStore.getQuota(id);
        } catch (Exception e) {
            String message = "Couldn't insert the quota: " + e.getMessage();
            throw new WebApplicationException(message, Response.Status.EXPECTATION_FAILED);
        }
        return quota;
    }

    /**
     * Get the quota given an id
     * @param quotaId  the quota id
     * @return  the quota for the id
     */
    @Timed
    @GET
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{quotaId}")
    public Quota retrieve(
        @Context SecurityContext context,
        @PathParam("quotaId") @NotNull Integer quotaId)
        throws WebApplicationException {

        Customer caller = (Customer) context.getUserPrincipal();

        Quota quota = null;
        // Get the quota from the store
        try {
            quota = quotaStore.getQuota(quotaId);

            if ( this.dataONEAuthHelper.isAdmin(caller.getSubject()) ) {
                return quota;
            }
            // Ensure the caller is asssociated with the quota subject
            String quotaSubject = quota.getSubject();
            List<String> subjects = new ArrayList<String>();
            subjects.add(quotaSubject);
            List<String> associatedSubjects =
                this.dataONEAuthHelper.getAssociatedSubjects(caller, subjects);
            if ( associatedSubjects.size() > 0 ) {
                return quota;
            } else {
                throw new Exception(caller.getSubject() + " is not associated with this quota.");
            }
        } catch (Exception e) {
            String message = "Couldn't get the quota: " + e.getMessage();
            throw new WebApplicationException(message, Response.Status.NOT_FOUND);
        }
    }

    /**
     * Update the quota
     * @param quota  the quota to update
     * @return  the updated quota
     * @throws WebApplicationException  a web app exception
     */
    @Timed
    @PUT
    @RolesAllowed("CN=urn:node:CN,DC=dataone,DC=org")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{quotaId}")
    public Quota update(
        @Context SecurityContext context,
        @NotNull @Valid Quota quota) throws WebApplicationException {
        // Update the quota after validation
        try {
            quotaStore.update(quota);
        } catch (Exception e) {
            String message = "Couldn't update the quota: " + e.getMessage();
            throw new WebApplicationException(message, Response.Status.EXPECTATION_FAILED);
        }
        return quota;
    }

    /**
     * Delete the quota
     * @param quotaId  the quota id
     * @return  response 200 if deleted
     * @throws WebApplicationException  a web app exception
     */
    @Timed
    @DELETE
    @RolesAllowed("CN=urn:node:CN,DC=dataone,DC=org")
    @Path("{quotaId}")
    public Response delete(
        @Context SecurityContext context,
        @PathParam("quotaId") @Valid Integer quotaId) throws WebApplicationException {
        String message = "The quotaId cannot be null.";
        if (quotaId == null) {
            throw new WebApplicationException(message, Response.Status.BAD_REQUEST);
        }
        try {
            quotaStore.delete(quotaId);
        } catch (Exception e) {
            message = "Deleting the quota with id " + quotaId + " failed: " + e.getMessage();
            log.error(message);
            e.printStackTrace();
            throw e;
        }
        return Response.ok().build();
    }
}
