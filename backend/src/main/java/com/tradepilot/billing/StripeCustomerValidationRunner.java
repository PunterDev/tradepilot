package com.tradepilot.billing;

import com.stripe.Stripe;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.tradepilot.entity.User;
import com.tradepilot.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StripeCustomerValidationRunner implements ApplicationRunner {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StripeCustomerValidationRunner.class);
    private final StripeProperties stripeProperties;
    private final StripeApiClient stripeApiClient;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        long usersWithStripeCustomerId = userRepository.countByStripeCustomerIdIsNotNull();
        log.info("Stripe customer ids stored: {}", usersWithStripeCustomerId);
        if (!StringUtils.hasText(stripeProperties.getSecretKey()) || usersWithStripeCustomerId == 0) {
            return;
        }
        Stripe.apiKey = stripeProperties.getSecretKey();
        long invalidCount = 0;
        for (User user : userRepository.findByStripeCustomerIdIsNotNull()) {
            try {
                Customer customer = stripeApiClient.retrieveCustomer(user.getStripeCustomerId());
                if (customer == null || Boolean.TRUE.equals(customer.getDeleted())) {
                    invalidCount++;
                }
            } catch (InvalidRequestException ex) {
                if (isNoSuchCustomer(ex)) {
                    invalidCount++;
                }
            } catch (StripeException ex) {
                log.info("Stripe customer validation skipped after Stripe error: {}", ex.getMessage());
                return;
            }
        }
        if (invalidCount > 0) {
            log.warn("Detected {} invalid stored Stripe customer ids for this environment.", invalidCount);
        }
    }

    private boolean isNoSuchCustomer(InvalidRequestException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("No such customer");
    }

    @java.lang.SuppressWarnings("all")
    public StripeCustomerValidationRunner(final StripeProperties stripeProperties, final StripeApiClient stripeApiClient, final UserRepository userRepository) {
        this.stripeProperties = stripeProperties;
        this.stripeApiClient = stripeApiClient;
        this.userRepository = userRepository;
    }
}
