package com.tradepilot.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.tradepilot.entity.User;

public interface StripeApiClient {
    Customer retrieveCustomer(String customerId) throws StripeException;

    Subscription retrieveSubscription(String subscriptionId) throws StripeException;

    Invoice retrieveInvoice(String invoiceId) throws StripeException;

    Customer createCustomer(User user) throws StripeException;

    Session createCheckoutSession(User user, String customerId, String proPriceId, String frontendUrl) throws StripeException;

    String createPortalSession(String customerId, String frontendUrl) throws StripeException;
}
