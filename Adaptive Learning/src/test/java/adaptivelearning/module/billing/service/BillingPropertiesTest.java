package adaptivelearning.module.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BillingPropertiesTest {
    @Test
    void acceptsOnlyPositiveWholeVndPrices() {
        BillingProperties properties = new BillingProperties();
        ReflectionTestUtils.setField(properties, "premiumPriceValue", "125000");

        assertEquals("VND", properties.premiumCurrency());
        assertEquals("125000", properties.premiumPrice().toPlainString());

        ReflectionTestUtils.setField(properties, "premiumPriceValue", "125000.50");
        assertNull(properties.premiumPrice());
        ReflectionTestUtils.setField(properties, "premiumPriceValue", "0");
        assertNull(properties.premiumPrice());
    }

    @Test
    void disablesCheckoutWhenConfiguredCurrencyIsNotVnd() {
        BillingProperties properties = new BillingProperties();
        ReflectionTestUtils.setField(properties, "premiumPriceValue", "12.00");
        ReflectionTestUtils.setField(properties, "premiumCurrencyValue", "USD");

        assertNull(properties.premiumPrice());
    }
}
