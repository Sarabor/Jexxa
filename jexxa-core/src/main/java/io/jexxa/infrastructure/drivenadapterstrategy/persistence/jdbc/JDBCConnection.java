package io.jexxa.infrastructure.drivenadapterstrategy.persistence.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import io.jexxa.utils.JexxaLogger;
import io.jexxa.utils.function.ThrowingConsumer;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class JDBCConnection implements AutoCloseable
{
    public static final String JDBC_URL = "io.jexxa.jdbc.url";
    public static final String JDBC_USERNAME = "io.jexxa.jdbc.username";
    public static final String JDBC_PASSWORD = "io.jexxa.jdbc.password";
    public static final String JDBC_DRIVER = "io.jexxa.jdbc.driver";
    public static final String JDBC_AUTOCREATE_DATABASE = "io.jexxa.jdbc.autocreate.database";
    public static final String JDBC_AUTOCREATE_TABLE = "io.jexxa.jdbc.autocreate.table";

    public static final int NO_TIMEOUT = 0;

    private Connection connection;
    private final Properties properties;

    private static final Logger LOGGER = JexxaLogger.getLogger(JDBCConnection.class);

    public JDBCConnection(Properties properties)
    {
        validateProperties(properties);

        initDBDriver(properties);

        autocreateDatabase(properties);

        this.connection = initJDBCConnection(properties);
        this.properties = properties;
    }

    public void autocreateDatabase(final Properties properties)
    {
        if (properties.containsKey(JDBC_AUTOCREATE_DATABASE))
        {
            var splitURL = properties.getProperty(JDBC_URL).split("/");
            var dbName = splitURL[splitURL.length - 1].toLowerCase(Locale.ENGLISH); //last part of the URL is the name of the database (Note: Some DBs such as postgres require a name in lower case!)

            Properties creationProperties = new Properties();
            creationProperties.putAll(properties);

            try (var setupConnection = DriverManager.
                    getConnection(
                            creationProperties.getProperty(JDBC_AUTOCREATE_DATABASE),
                            creationProperties.getProperty(JDBC_USERNAME),
                            creationProperties.getProperty(JDBC_PASSWORD));
                 Statement statement = setupConnection.createStatement())
            {
                setupConnection.setAutoCommit(true);
                statement.execute(String.format("create DATABASE %s ", dbName));
                LOGGER.info("Database {} successfully created ", dbName);
            }
            catch (SQLException e)
            {
                LOGGER.warn("Could not create database {} => Assume that database already exists", dbName);
            }
        }
    }

    @SuppressWarnings("java:S2139") // Here we log and and rethrow an exception in order to document that we tried to handle a connection failure without success and must give up
    public final JDBCConnection validateConnection()
    {
        try
        {
            if (!isValid())
            {
                LOGGER.warn("JDBC connection for connection {} is invalid. ", properties.getProperty(JDBC_URL));
                LOGGER.warn("Try to reset JDBC connection for connection {}",  properties.getProperty(JDBC_URL));
                reset();
                LOGGER.warn("JDBC connection for connection {} successfully restarted.",  properties.getProperty(JDBC_URL));
            }
        } catch (RuntimeException e)
        {
            LOGGER.error("Could not reset JDBC connection for connection {}. Reason: {}", properties.getProperty(JDBC_URL), e.getMessage());
            throw e;
        }

        return this;
    }

    public JDBCCommand execute(String command)
    {
        return new JDBCCommand(this::validateConnection, command);
    }

    public JDBCQuery query(String sqlQuery)
    {
        return new JDBCQuery(this::validateConnection, sqlQuery);
    }


    /**
     * This method resets the internal JDBC connection in the following way:
     * <ol>
     *  <li>The existing JDBC connection is closed.</li>
     *  <li>A new JDBC connection is established based on the given properties in constructor.</li>
     *  <li>The new JDBC connection is validated using {@link Connection#isValid(int) }.</li>
     * </ol>
     * If any of these steps fails, an IllegalStateException is thrown including the error message from JDBC driver.
     */
    public void reset()
    {
        close();
        try
        {
            if (!getConnection().isValid(NO_TIMEOUT))
            {
                throw new IllegalStateException("JDBC Connection is invalid for connection " + properties.getProperty(JDBC_URL));
            }
        } catch (SQLException e)
        {
            throw new IllegalStateException("Could not reset JDCConnection. Reason: " + e.getMessage(), e);
        }
    }

    public boolean isValid()
    {
        return isValid(NO_TIMEOUT);
    }

    public boolean isValid(int timeout)
    {
        if ( connection == null )
        {
            return false;
        }
        try
        {
            return connection.isValid(timeout);
        } catch (SQLException e)
        {
            return false;
        }
    }

    /**
     * Creates a PreparedStatement
     *
     * @param sqlStatement describes the template of the command
     * @return PreparedStatement
     * @throws SQLException in case of an error
     * @deprecated Inside this package use {@link #createStatement()}. Outside of this package use {@link JDBCCommand} or {@link JDBCQuery}
     */
    @Deprecated(forRemoval = true)
    public PreparedStatement prepareStatement(String sqlStatement) throws SQLException
    {
        return getConnection().prepareStatement(sqlStatement);
    }

    Statement createStatement() throws SQLException
    {
        return getConnection().createStatement();
    }

    public void close()
    {
        Optional.ofNullable(connection)
                .ifPresent(ThrowingConsumer.exceptionLogger(Connection::close));
        connection = null;
    }

    protected Connection getConnection()
    {
        if ( connection == null )
        {
            connection = initJDBCConnection(properties);
        }
        return connection;
    }

    private static Connection initJDBCConnection(final Properties properties)
    {

        try {
            var connection = DriverManager.getConnection(
                    properties.getProperty(JDBC_URL),
                    properties.getProperty(JDBC_USERNAME),
                    properties.getProperty(JDBC_PASSWORD)
            );

            connection.setAutoCommit(true);
            return connection;
        }
        catch (SQLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }


    private static void initDBDriver(Properties properties)
    {
        try
        {
            Class.forName(properties.getProperty(JDBC_DRIVER));
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalArgumentException("Specified JDBC driver is not available: " + properties.getProperty(JDBC_DRIVER), e);
        }
    }

    private static void validateProperties(Properties properties)
    {
        Validate.isTrue(properties.containsKey(JDBC_URL), "Parameter " + JDBC_URL + " is missing");
        Validate.isTrue(properties.containsKey(JDBC_DRIVER), "Parameter " + JDBC_DRIVER + " is missing");
    }
}
