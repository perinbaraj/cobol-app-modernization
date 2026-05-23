package com.acme.customer.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded phone record.
 * Migrated from CUST-PHONES OCCURS 3 TIMES in CUST-REC copybook:
 * - 10 PHONE-TYPE PIC X(1)
 * - 10 PHONE-NUMBER PIC X(15)
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPhone {

    @Enumerated(EnumType.STRING)
    private PhoneType phoneType;

    @Size(max = 15)
    private String phoneNumber;
}
