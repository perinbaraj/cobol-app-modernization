package com.acme.customer.service;

import com.acme.customer.dto.CustomerRequest;
import com.acme.customer.dto.CustomerResponse;
import com.acme.customer.exception.CustomerException.*;
import com.acme.customer.model.Customer;
import com.acme.customer.model.CustomerPhone;
import com.acme.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Customer service implementing business logic.
 * Migrated from CUSTMGMT.cbl COBOL program.
 *
 * <pre>
 * COBOL Paragraph Mapping:
 * - 1000-ADD-CUSTOMER    → createCustomer()
 * - 2000-UPDATE-CUSTOMER → updateCustomer()
 * - 3000-DELETE-CUSTOMER → deleteCustomer()
 * - 4000-INQUIRE-CUSTOMER → getCustomer()
 * - 5000-POPULATE-RECORD → populateCustomerFromRequest() (private)
 * - 6000-APPLY-DISCOUNT  → applyDiscount() (private)
 * </pre>
 */
@Service
@Transactional
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    /**
     * Discount threshold - customers with balance > 10000.00 get discount.
     * COBOL: IF CUST-BALANCE > 10000.00
     */
    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("10000.00");

    /**
     * Discount rate - 5% (0.0500).
     * COBOL: WS-DISCOUNT-RATE PIC 9V9(4) COMP-3 VALUE 0.0500
     */
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.0500");

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Create a new customer.
     * Migrated from 1000-ADD-CUSTOMER paragraph.
     *
     * @param request the customer request DTO
     * @return the created customer response
     * @throws CustomerAlreadyExistsException if customer already exists (COBOL code 1002)
     */
    public CustomerResponse createCustomer(CustomerRequest request) {
        logger.info("Creating customer: {}", request.customerId());

        // Check if customer already exists (COBOL: READ with NOT INVALID KEY)
        if (customerRepository.existsByCustomerId(request.customerId())) {
            logger.warn("Customer already exists: {}", request.customerId());
            throw new CustomerAlreadyExistsException(request.customerId());
        }

        // Populate record (5000-POPULATE-RECORD)
        Customer customer = populateCustomerFromRequest(request, new Customer());

        // Apply discount if eligible (6000-APPLY-DISCOUNT)
        applyDiscount(customer);

        // Save (COBOL: WRITE CUSTOMER-RECORD)
        try {
            Customer saved = customerRepository.save(customer);
            logger.info("Customer created successfully: {}", saved.getCustomerId());
            return CustomerResponse.fromEntity(saved);
        } catch (Exception e) {
            logger.error("Failed to create customer: {}", request.customerId(), e);
            throw new CustomerPersistenceException("WRITE", 1003, e);
        }
    }

    /**
     * Update an existing customer.
     * Migrated from 2000-UPDATE-CUSTOMER paragraph.
     *
     * @param customerId the customer ID
     * @param request    the customer request DTO
     * @return the updated customer response
     * @throws CustomerNotFoundException if customer not found (COBOL code 2001)
     */
    public CustomerResponse updateCustomer(String customerId, CustomerRequest request) {
        logger.info("Updating customer: {}", customerId);

        // Find existing customer (COBOL: READ with INVALID KEY)
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    logger.warn("Customer not found for update: {}", customerId);
                    return new CustomerNotFoundException(customerId);
                });

        // Populate record (5000-POPULATE-RECORD)
        populateCustomerFromRequest(request, customer);

        // Apply discount if eligible (6000-APPLY-DISCOUNT)
        applyDiscount(customer);

        // Save (COBOL: REWRITE CUSTOMER-RECORD)
        try {
            Customer saved = customerRepository.save(customer);
            logger.info("Customer updated successfully: {}", saved.getCustomerId());
            return CustomerResponse.fromEntity(saved);
        } catch (Exception e) {
            logger.error("Failed to update customer: {}", customerId, e);
            throw new CustomerPersistenceException("REWRITE", 2002, e);
        }
    }

    /**
     * Delete a customer.
     * Migrated from 3000-DELETE-CUSTOMER paragraph.
     *
     * @param customerId the customer ID
     * @throws CustomerNotFoundException if customer not found (COBOL code 3001)
     */
    public void deleteCustomer(String customerId) {
        logger.info("Deleting customer: {}", customerId);

        // Check if customer exists (COBOL: READ with INVALID KEY)
        if (!customerRepository.existsByCustomerId(customerId)) {
            logger.warn("Customer not found for delete: {}", customerId);
            throw new CustomerNotFoundException(customerId);
        }

        // Delete (COBOL: DELETE CUSTOMER-FILE)
        try {
            customerRepository.deleteById(customerId);
            logger.info("Customer deleted successfully: {}", customerId);
        } catch (Exception e) {
            logger.error("Failed to delete customer: {}", customerId, e);
            throw new CustomerPersistenceException("DELETE", 3002, e);
        }
    }

    /**
     * Get a customer by ID.
     * Migrated from 4000-INQUIRE-CUSTOMER paragraph.
     *
     * @param customerId the customer ID
     * @return the customer response
     * @throws CustomerNotFoundException if customer not found (COBOL code 4001)
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(String customerId) {
        logger.debug("Inquiring customer: {}", customerId);

        // Find customer (COBOL: READ with INVALID KEY)
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    logger.warn("Customer not found for inquiry: {}", customerId);
                    return new CustomerNotFoundException(customerId);
                });

        // Return data (COBOL: MOVE fields to LS-CUST-DATA)
        return CustomerResponse.fromEntity(customer);
    }

    /**
     * Get all customers.
     *
     * @return list of all customer responses
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        logger.debug("Retrieving all customers");
        return customerRepository.findAll().stream()
                .map(CustomerResponse::fromEntity)
                .toList();
    }

    /**
     * Populate customer entity from request DTO.
     * Migrated from 5000-POPULATE-RECORD paragraph.
     *
     * @param request  the customer request DTO
     * @param customer the customer entity to populate
     * @return the populated customer entity
     */
    private Customer populateCustomerFromRequest(CustomerRequest request, Customer customer) {
        customer.setCustomerId(request.customerId());
        customer.setCustomerName(request.customerName());
        customer.setCustomerAddress(request.customerAddress());
        customer.setCustomerCity(request.customerCity());
        customer.setCustomerState(request.customerState());
        customer.setCustomerZip(request.customerZip());
        customer.setCustomerBalance(request.customerBalance() != null 
                ? request.customerBalance() 
                : BigDecimal.ZERO);
        customer.setCustomerStatus(request.customerStatus());

        // Map phones (OCCURS 3 TIMES)
        if (request.phones() != null) {
            List<CustomerPhone> phones = request.phones().stream()
                    .map(p -> new CustomerPhone(p.phoneType(), p.phoneNumber()))
                    .toList();
            customer.setPhones(phones);
        }

        return customer;
    }

    /**
     * Apply discount to customer balance if eligible.
     * Migrated from 6000-APPLY-DISCOUNT paragraph.
     *
     * <pre>
     * COBOL Logic:
     * IF CUST-BALANCE > 10000.00
     *     COMPUTE WS-DISCOUNT-AMOUNT = CUST-BALANCE * WS-DISCOUNT-RATE
     *     SUBTRACT WS-DISCOUNT-AMOUNT FROM CUST-BALANCE
     *         ON SIZE ERROR
     *             MOVE 'DISCOUNT CALC OVERFLOW' TO WS-ERROR-MSG
     *             MOVE 6001 TO WS-RETURN-CODE
     *     END-SUBTRACT
     * END-IF
     * </pre>
     *
     * @param customer the customer entity
     * @throws DiscountCalculationException if calculation overflow occurs (COBOL code 6001)
     */
    private void applyDiscount(Customer customer) {
        BigDecimal balance = customer.getCustomerBalance();
        
        if (balance == null) {
            return;
        }

        // Check if balance exceeds threshold
        if (balance.compareTo(DISCOUNT_THRESHOLD) > 0) {
            try {
                // Calculate discount: balance * rate (using BigDecimal for COMP-3 precision)
                BigDecimal discountAmount = balance
                        .multiply(DISCOUNT_RATE)
                        .setScale(2, RoundingMode.HALF_UP);

                // Subtract discount from balance
                BigDecimal newBalance = balance.subtract(discountAmount);

                // Validate result (equivalent to ON SIZE ERROR)
                // PIC S9(7)V99 can hold max 9999999.99
                BigDecimal maxValue = new BigDecimal("9999999.99");
                BigDecimal minValue = new BigDecimal("-9999999.99");
                
                if (newBalance.compareTo(maxValue) > 0 || newBalance.compareTo(minValue) < 0) {
                    throw new DiscountCalculationException("DISCOUNT CALC OVERFLOW");
                }

                customer.setCustomerBalance(newBalance);
                logger.debug("Applied discount of {} to customer {}, new balance: {}",
                        discountAmount, customer.getCustomerId(), newBalance);

            } catch (ArithmeticException e) {
                logger.error("Discount calculation overflow for customer: {}", customer.getCustomerId());
                throw new DiscountCalculationException("DISCOUNT CALC OVERFLOW");
            }
        }
    }
}
