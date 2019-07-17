package org.dataone.bookkeeper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jdbi3.strategies.TimedAnnotationNameStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.setup.Environment;
import org.dataone.bookkeeper.api.Quota;
import org.eclipse.jetty.util.component.LifeCycle;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * A base class for initializing an embedded database for testing
 */
public class BaseTestCase {

    // The embedded database reference
    public static EmbeddedPostgres pg = EmbeddedPostgresqlExtension.pg;

    // A connection to the database
    public static Connection connection = EmbeddedPostgresqlExtension.connection;

    // The Flyway database migrator used to manage database schema integrity
    public static Flyway flyway = EmbeddedPostgresqlExtension.flyway;

    // The metrics registry for testing
    public static MetricRegistry metricRegistry = new MetricRegistry();

    // The Dropwizard environment used in tests
    public static Environment environment;

    // The data source factory used to get a postgresql datasource
    public static DataSourceFactory dataSourceFactory = new DataSourceFactory();

    // The JDBI instance used in testing
    public static Jdbi dbi;

    /**
     * Initialize test resources - start an embedded PostgreSQL database
     */

    @BeforeAll
    public static void initAll() {
        try {

            // Try to optimize the PG database for testing with anti-persistence
            // options (fsync, full_page_writes)
            pg = EmbeddedPostgres.builder()
                .setPort(5432)
                .setServerConfig("shared_buffers", "1024MB")
                .setServerConfig("work_mem", "25MB")
                .setServerConfig("fsync", "off")
                .setServerConfig("full_page_writes", "off")
                .start();

            // Make a connection available to tests
            connection = pg.getPostgresDatabase().getConnection();

            // Run the production database migrations
            flyway = Flyway.configure()
                .dataSource(pg.getPostgresDatabase())
                .locations("db/migrations")
                .load();
            flyway.migrate();

            // Create a Dropwizard environment for testing
            environment = new Environment("bookkeeper",
                new ObjectMapper(), Validators.newValidator(), metricRegistry,
                ClassLoader.getSystemClassLoader());

            // Set up a PostgreSQL datasource for testing (DAOs)

            dataSourceFactory.setUrl("jdbc:postgresql://localhost:5432/postgres");
            dataSourceFactory.setUser("postgres");
            dataSourceFactory.setPassword("postgres");
            dataSourceFactory.setDriverClass("org.postgresql.Driver");
            dataSourceFactory.asSingleConnectionPool();

            // Initialize a dbi instance for tests to use
            dbi = new JdbiFactory(new TimedAnnotationNameStrategy())
                .build(environment, dataSourceFactory, "postgresql");

            // Start all managed objects in the environment
            for (LifeCycle lifeCycle : environment.lifecycle().getManagedObjects() ) {
                lifeCycle.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Clean up after all tests as needed
     */

    @AfterAll
    public static void tearDownAll() {

        try {

            // Stop all managed objects in the environment
            for (LifeCycle lifeCycle : environment.lifecycle().getManagedObjects() ) {
                lifeCycle.stop();
            }

            // Clean the database
            flyway.clean();

            // Close the database
            pg.close();

        } catch (IOException e) {
            e.printStackTrace();
            fail();

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}