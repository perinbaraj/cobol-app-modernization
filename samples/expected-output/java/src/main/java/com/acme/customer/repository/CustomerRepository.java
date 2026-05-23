package com.acme.customer.repository;

import com.acme.customer.model.Customer;
import com.acme.customer.model.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Customer repository for data access.
 * Migrated from COBOL file operations in CUSTMGMT.cbl:
 * - READ CUSTOMER-FILE → findById()
 * - WRITE CUSTOMER-RECORD → save()
 * - REWRITE CUSTOMER-RECORD → save()
 * - DELETE CUSTOMER-FILE → deleteById()
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Find customers by status.
     * Equivalent to COBOL file read with status filter.
     *
     * @param status the customer status
     * @return list of customers with the given status
     */
    List<Customer> findByCustomerStatus(CustomerStatus status);

    /**
     * Find customers with balance greater than threshold.
     * Used for discount eligibility (6000-APPLY-DISCOUNT paragraph).
     *
     * @param balance the balance threshold
     * @return list of customers with balance above threshold
     */
    @Query("SELECT c FROM Customer c WHERE c.customerBalance > :balance")
    List<Customer> findByBalanceGreaterThan(@Param("balance") BigDecimal balance);

    /**
     * Find customers by state.
     *
     * @param state the state code
     * @return list of customers in the given state
     */
    List<Customer> findByCustomerState(String state);

    /**
     * Check if customer exists by ID.
     * Equivalent to READ with INVALID KEY check in COBOL.
     *
     * @param customerId the customer ID
     * @return true if customer exists
     */
    boolean existsByCustomerId(String customerId);
}
