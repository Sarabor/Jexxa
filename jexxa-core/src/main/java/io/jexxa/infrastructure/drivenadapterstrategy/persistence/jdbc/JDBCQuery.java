package io.jexxa.infrastructure.drivenadapterstrategy.persistence.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class JDBCQuery
{
    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    private static final String INVALID_QUERY = "Invalid query or type conversion: ";

    private final Supplier<JDBCConnection> jdbcConnection;
    private final String command;

    JDBCQuery(Supplier<JDBCConnection> jdbcConnection, String command)
    {
        Objects.requireNonNull(jdbcConnection);
        Objects.requireNonNull(command);

        this.jdbcConnection = jdbcConnection;
        this.command = command;
    }

    public Stream<Optional<String>> asString()
    {
        return as( resultSet -> resultSet.getString(1) ).map(Optional::ofNullable);
    }

    public Stream<Optional<BigDecimal>> asNumeric()
    {
        return as( resultSet -> resultSet.getBigDecimal(1) ).map(Optional::ofNullable);
    }

    public Stream<Long> asLong()
    {
        return as( resultSet -> resultSet.getLong(1) );
    }

    public Stream<Float> asFloat()
    {
        return as( resultSet -> resultSet.getFloat(1) );
    }

    public Stream<Double> asDouble()
    {
        return as( resultSet -> resultSet.getDouble(1) );
    }

    public Stream<Integer> asInt()
    {
        return as( resultSet -> resultSet.getInt(1) );
    }

    public Stream<Optional<Timestamp>> asTimestamp()
    {
        return as(resultSet -> resultSet.getTimestamp(1))
                .map(Optional::ofNullable);
    }

    public boolean isEmpty()
    {
        return !isPresent();
    }

    public boolean isPresent()
    {
        try ( var statement = jdbcConnection.get().createStatement();
              var resultSet = statement.executeQuery(command))
        {
            return resultSet.next();
        }
        catch (SQLException e)
        {
            throw new IllegalStateException(INVALID_QUERY + command , e);
        }
    }

    public <R> Stream<R> as(CheckedFunction<ResultSet, R> function)
    {
        try ( var statement = jdbcConnection.get().createStatement();
              var resultSet = statement.executeQuery(command))
        {
            List<R> result = new ArrayList<>();
            while ( resultSet.next() )
            {
                result.add(function.apply(resultSet));
            }
            return result.stream();
        }
        catch (SQLException e)
        {
            throw new IllegalStateException(INVALID_QUERY + command , e);
        }
    }
}