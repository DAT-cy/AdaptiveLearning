package adaptivelearning.module.billing.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BillingProperties {
    @Value("${PREMIUM_PRICE:}")
    private String premiumPriceValue;

    @Value("${PREMIUM_CURRENCY:USD}")
    private String premiumCurrencyValue;

    @Value("${FREE_EXAM_LIMIT:5}")
    private int freeExamLimitValue;

    @Value("${APP_URL:http://localhost:3000}")
    private String appUrlValue;

    @Value("${API_URL:http://localhost:9090}")
    private String apiUrlValue;

    public BigDecimal premiumPrice() {
        if (premiumPriceValue == null || premiumPriceValue.isBlank()) return null;
        try {
            BigDecimal price = new BigDecimal(premiumPriceValue.trim()).setScale(2, RoundingMode.UNNECESSARY);
            return price.signum() > 0 ? price : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public String premiumCurrency() {
        return premiumCurrencyValue == null || premiumCurrencyValue.isBlank()
                ? "USD" : premiumCurrencyValue.trim().toUpperCase(Locale.ROOT);
    }

    public int freeExamLimit() {
        return Math.max(1, freeExamLimitValue);
    }

    public String appUrl() {
        String value = appUrlValue == null || appUrlValue.isBlank() ? "http://localhost:3000" : appUrlValue.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public String apiUrl() {
        String value = apiUrlValue == null || apiUrlValue.isBlank() ? "http://localhost:9090" : apiUrlValue.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
