package com.acme.customer.dto;

import com.acme.customer.model.CustomerStatus;
import com.acme.customer.model.PhoneType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Customer request DTO for create/update operations.
 * Validation constraints match COBOL PIC sizes from CUST-REC copybook.
 *
 * @param customerId      Customer ID (PIC X(10))
 * @param customerName    Customer name (PIC X(30))
 * @param customerAddress Customer address (PIC X(40))
 * @param customerCity    Customer city (PIC X(20))
 * @param customerState   Customer state (PIC X(2))
 * @param customerZip     Customer ZIP code (PIC X(10))
 * @param customerBalance Customer balance (PIC S9(7)V99 COMP-3)
 * @param customerStatus  Customer status (88-levels: A, I, S)
 * @param phones          Customer phone numbers (OCCURS 3 TIMES)
 */
public record CustomerRequest(
        @NotBlank(message = "Customer ID is required")
        @Size(max = 10, message = "Customer ID must not exceed 10 characters")
        String customerId,

        @NotBlank(message = "Customer name is required")
        @Size(max = 30, message = "Customer name must not exceed 30 characters")
        String customerName,

        @Size(max = 40, message = "Customer address must not exceed 40 characters")
        String customerAddress,

        @Size(max = 20, message = "Customer city must not exceed 20 characters")
        String customerCity,

        @Size(max = 2, message = "Customer state must not exceed 2 characters")
        String customerState,

        @Size(max = 10, message = "Customer ZIP must not exceed 10 characters")
        String customerZip,

        @Digits(integer = 7, fraction = 2, message = "Balance must have at most 7 integer digits and 2 decimal places")
        BigDecimal customerBalance,

        @NotNull(message = "Customer status is required")
        CustomerStatus customerStatus,

        @Valid
        @Size(max = 3, message = "Maximum 3 phone numbers allowed")
        List<PhoneRequest> phones
) {
    /**
     * Phone request DTO.
     * Matches CUST-PHONES layout from CUST-REC copybook.
     *
     * @param phoneType   Phone type (88-levels: H, W, M)
     * @param phoneNumber Phone number (PIC X(15))
     */
    public record PhoneRequest(
            @NotNull(message = "Phone type is required")
            PhoneType phoneType,

            @NotBlank(message = "Phone number is required")
            @Size(max = 15, message = "Phone number must not exceed 15 characters")
            String phoneNumber
    ) {
    }
}
