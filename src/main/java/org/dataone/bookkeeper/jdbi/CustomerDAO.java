package org.dataone.bookkeeper.jdbi;

import org.dataone.bookkeeper.api.Customer;
import org.dataone.bookkeeper.api.Quota;
import org.dataone.bookkeeper.jdbi.mappers.CustomerMapper;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import java.util.List;

/**
 * The customer data access interfaces used to create, read, update, and delete
 * customers from the database
 */
public interface CustomerDAO {

    /** The query used to find all customers with their quotas */
    String SELECT_ALL =
        "SELECT " +
            "c.id AS c_id, c.object AS c_object, c.orcid AS c_orcid, c.balance AS c_balance, " +
            "c.address AS c_address, date_part('epoch', c.created)::int AS c_created, " +
            "c.currency AS c_currency, c.delinquent AS c_delinquent, " +
            "c.description AS c_description, c.discount::json AS c_discount, " +
            "c.email AS c_email, c.invoicePrefix AS c_invoicePrefix, " +
            "c.invoiceSettings::json AS c_invoiceSettings, " +
            "c.metadata::json AS c_metadata, c.givenName AS c_givenName, " +
            "c.surName AS c_surName, c.phone AS c_phone, " +
            "q.id, q.object, q.name, q.softLimit, q.hardLimit, q.unit, q.customerId " +
        "FROM customers c " +
        "LEFT JOIN quotas q " +
        "ON c.id = q.customerId " +
        "ORDER BY c.surName, c.givenName, q.name ";

    /** The query used to find an individual customer */
    String SELECT_ONE = SELECT_ALL + "WHERE c.id = :id";

    /**
     * Interface to list all customers with their quotas
     * @return customers The list of customers
     */
    @SqlQuery(SELECT_ALL)
    @RegisterRowMapper(CustomerMapper.class)
    @RegisterBeanMapper(value = Quota.class)
    @UseRowReducer(CustomerQuotasReducer.class)
    List<Customer> listCustomers();

    /**
     * Interface to get a individual customer
     * @return customer The individual customer
     */
    @SqlQuery(SELECT_ONE)
    @RegisterRowMapper(CustomerMapper.class)
    @RegisterBeanMapper(value = Quota.class)
    @UseRowReducer(CustomerQuotasReducer.class)
    Customer getCustomer(@Bind("id") Integer id);
}