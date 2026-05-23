package com.acme.customer.dto;

import com.acme.customer.model.Customer;
import com.acme.customer.model.CustomerPhone;
import com.acme.customer.model.CustomerStatus;
import com.acme.customer.model.PhoneType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Customer response DTO for API responses.
 * Provides a clean API contract separate from the JPA entity.
 *
 * @param customerId      Customer ID
 * @param customerName    Customer name
 * @param customerAddress Customer address
 * @param customerCity    Customer city
 * @param customerState   Customer state
 * @param customerZip     Customer ZIP code
 * @param customerBalance Customer balance (BigDecimal for COMP-3 precision)
 * @param customerStatus  Customer status enum
 * @param lastUpdateDate  Last update date
 * @param phones          Customer phone numbers
 */
public record CustomerResponse(
        String customerId,
        String customerName,
        String customerAddress,
        String customerCity,
        String customerState,
        String customerZip,
        BigDecimal customerBalance,
        CustomerStatus customerStatus,
        LocalDate lastUpdateDate,
        List<PhoneResponse> phones
) {
    /**
     * Phone response DTO.
     *
     * @param phoneType   Phone type enum
     * @param phoneNumber Phone number
     */
    public record PhoneResponse(
            PhoneType phoneType,
            String phoneNumber
    ) {
        /**
         * Create PhoneResponse from CustomerPhone entity.
         *
         * @param phone the CustomerPhone entity
         * @return PhoneResponse DTO
         */
        public static PhoneResponse fromEntity(CustomerPhone phone) {
            return new PhoneResponse(
                    phone.getPhoneType(),
                    phone.getPhoneNumber()
            );
        }
    }

    /**
     * Create CustomerResponse from Customer entity.
     *
     * @param customer the Customer entity
     * @return CustomerResponse DTO
     */
    public static CustomerResponse fromEntity(Customer customer) {
        List<PhoneResponse> phoneResponses = customer.getPhones() != null
                ? customer.getPhones().stream()
                    .map(PhoneResponse::fromEntity)
                    .toList()
                : List.of();

        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getCustomerName(),
                customer.getCustomerAddress(),
                customer.getCustomerCity(),
                customer.getCustomerState(),
                customer.getCustomerZip(),
                customer.getCustomerBalance(),
                customer.getCustomerStatus(),
                customer.getLastUpdateDate(),
                phoneResponses
        );
    }
}
