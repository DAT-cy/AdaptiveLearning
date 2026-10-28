package adaptivelearning.shared.enums;

import java.util.Locale;

public enum PaymentProvider {
    STRIPE,
    PAYPAL,
    SANDBOX;

    public static PaymentProvider from(String value) {
        return PaymentProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
