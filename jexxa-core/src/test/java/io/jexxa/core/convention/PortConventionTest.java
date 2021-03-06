package io.jexxa.core.convention;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import java.util.Properties;

import io.jexxa.application.applicationservice.InvalidApplicationService;
import io.jexxa.application.domainservice.IJexxaEntityRepository;
import org.junit.jupiter.api.Test;

class PortConventionTest
{
    @Test
    void invalidPortConstructor()
    {
        //Arrange - Nothing

        //Act/Assert
        assertThrows(PortConventionViolation.class, () -> PortConvention.validate(InvalidApplicationService.class)); // Violation: No public constructor
        assertThrows(PortConventionViolation.class, () -> PortConvention.validate(InvalidApplicationServiceNoInterface.class)); // Violation: Constructor does not take interfaces as argument
        assertThrows(PortConventionViolation.class, () -> PortConvention.validate(InvalidApplicationServiceMultipleConstructor.class)); // Violation: multiple constructor available
    }

    public static class InvalidApplicationServiceMultipleConstructor
    {
        @SuppressWarnings("unused")
        public InvalidApplicationServiceMultipleConstructor()
        {
            //Empty constructor for testing purpose
        }

        @SuppressWarnings("unused")
        public InvalidApplicationServiceMultipleConstructor(IJexxaEntityRepository jexxaAggregateRepository)
        {
            Objects.requireNonNull(jexxaAggregateRepository);
        }
    }

    public static class InvalidApplicationServiceNoInterface
    {
        public InvalidApplicationServiceNoInterface(Properties properties)
        {
            Objects.requireNonNull(properties);
        }

    }

}
