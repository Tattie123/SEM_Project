package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnitTest {

    @Test
    void testCityName() {
        String city = "London";

        assertEquals("London", city);
    }
}
