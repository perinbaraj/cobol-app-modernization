package com.acme.customer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity.
 * Migrated from CUST-REC copybook (CUST-DATA record layout).
 *
 * <pre>
 * Original COBOL layout:
 * 01 CUST-DATA.
 *    05 CUST-ID             PIC X(10).
 *    05 CUST-NAME           PIC X(30).
 *    05 CUST-ADDR           PIC X(40).
 *    05 CUST-CITY           PIC X(20).
 *    05 CUST-STATE          PIC X(2).
 *    05 CUST-ZIP            PIC X(10).
 *    05 CUST-BALANCE        PIC S9(7)V99 COMP-3.
 *    05 CUST-STATUS         PIC X(1).
 *    05 CUST-LAST-UPDATE    PIC 9(8).
 *    05 CUST-PHONE-COUNT    PIC 9(2).
 *    05 CUST-PHONES OCCURS 3 TIMES.
 * </pre>
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    /**
     * Customer ID - Primary key.
     * COBOL: PIC X(10)
     */
    @Id
    @Column(name = "cust_id", length = 10, nullable = false)
    @NotBlank
    @Size(max = 10)
    private String customerId;

    /**
     * Customer name.
     * COBOL: PIC X(30)
     */
    @Column(name = "cust_name", length = 30, nullable = false)
    @NotBlank
    @Size(max = 30)
    private String customerName;

    /**
     * Customer address.
     * COBOL: PIC X(40)
     */
    @Column(name = "cust_addr", length = 40)
    @Size(max = 40)
    private String customerAddress;

    /**
     * Customer city.
     * COBOL: PIC X(20)
     */
    @Column(name = "cust_city", length = 20)
    @Size(max = 20)
    private String customerCity;

    /**
     * Customer state.
     * COBOL: PIC X(2)
     */
    @Column(name = "cust_state", length = 2)
    @Size(max = 2)
    private String customerState;

    /**
     * Customer ZIP code.
     * COBOL: PIC X(10)
     */
    @Column(name = "cust_zip", length = 10)
    @Size(max = 10)
    private String customerZip;

    /**
     * Customer balance.
     * COBOL: PIC S9(7)V99 COMP-3 (packed decimal, 7 digits + 2 decimal places)
     * Using BigDecimal as per migration rules - NEVER use double/float for COBOL decimal fields.
     */
    @Column(name = "cust_balance", precision = 9, scale = 2)
    @Digits(integer = 7, fraction = 2)
    private BigDecimal customerBalance;

    /**
     * Customer status.
     * COBOL: PIC X(1) with 88-levels (A=Active, I=Inactive, S=Suspended)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cust_status", length = 10)
    private CustomerStatus customerStatus;

    /**
     * Last update date.
     * COBOL: PIC 9(8) - stored as YYYYMMDD
     */
    @Column(name = "cust_last_update")
    private LocalDate lastUpdateDate;

    /**
     * Customer phones.
     * COBOL: CUST-PHONES OCCURS 3 TIMES
     */
    @ElementCollection
    @CollectionTable(name = "customer_phones", joinColumns = @JoinColumn(name = "cust_id"))
    @Size(max = 3)
    @Builder.Default
    private List<CustomerPhone> phones = new ArrayList<>();

    /**
     * Audit field - created timestamp.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    /**
     * Audit field - updated timestamp.
     */
    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
        lastUpdateDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
        lastUpdateDate = LocalDate.now();
    }
}
