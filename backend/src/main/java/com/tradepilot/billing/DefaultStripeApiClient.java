package com.tradepilot.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.InvoiceRetrieveParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.tradepilot.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DefaultStripeApiClient implements StripeApiClient {
    @Override
    public Customer retrieveCustomer(String customerId) throws StripeException {
        return Customer.retrieve(customerId);
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

    @Override
    public Invoice retrieveInvoice(String invoiceId) throws StripeException {
        InvoiceRetrieveParams params = InvoiceRetrieveParams.builder()
                .addExpand("customer")
                .addExpand("subscription")
                .build();
        return Invoice.retrieve(invoiceId, params, null);
    }

    @Override
    public Customer createCustomer(User user) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getName())
                .putMetadata("userId", String.valueOf(user.getId()))
                .build();
        return Customer.create(params);
    }

    @Override
    public Session createCheckoutSession(User user, String customerId, String proPriceId, String frontendUrl) throws StripeException {
        return Session.create(subscriptionCheckoutParams(user, customerId, proPriceId, frontendUrl));
    }

    SessionCreateParams subscriptionCheckoutParams(User user, String customerId, String proPriceId, String frontendUrl) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setAllowPromotionCodes(true)
                .setSuccessUrl(frontendUrl + "/dashboard?billing=success")
                .setCancelUrl(frontendUrl + "/dashboard?billing=cancelled")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(proPriceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("userId", String.valueOf(user.getId()))
                .putMetadata("userEmail", user.getEmail())
                .build();
    }

    @Override
    public Session createCreditPackCheckoutSession(User user, String customerId, AiCreditPack pack, String frontendUrl) throws StripeException {
        return Session.create(creditPackCheckoutParams(user, customerId, pack, frontendUrl));
    }

    SessionCreateParams creditPackCheckoutParams(User user, String customerId, AiCreditPack pack, String frontendUrl) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomer(customerId)
                .setSuccessUrl(frontendUrl + "/billing?credits=success")
                .setCancelUrl(frontendUrl + "/billing?credits=cancelled")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(pack.currency())
                                .setUnitAmount((long) pack.amountCents())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("TradePilot AI Credit Pack - " + pack.credits() + " credits")
                                        .build())
                                .build())
                        .build())
                .putMetadata("type", "AI_CREDIT_PACK")
                .putMetadata("userId", String.valueOf(user.getId()))
                .putMetadata("userEmail", user.getEmail())
                .putMetadata("packId", pack.name())
                .putMetadata("credits", String.valueOf(pack.credits()))
                .build();
    }

    @Override
    public String createPortalSession(String customerId, String frontendUrl) throws StripeException {
        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(customerId)
                        .setReturnUrl(frontendUrl + "/dashboard")
                        .build();
        return com.stripe.model.billingportal.Session.create(params).getUrl();
    }
}
