package com.acme.customer.service;

import com.acme.customer.dto.CustomerRequest;
import com.acme.customer.dto.CustomerResponse;
import com.acme.customer.exception.CustomerException.*;
import com.acme.customer.model.Customer;
import com.acme.customer.model.CustomerPhone;
import com.acme.customer.model.CustomerStatus;
import com.acme.customer.model.PhoneType;
import com.acme.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerService.
 * Tests all CRUD operations migrated from CUSTMGMT.cbl paragraphs.
 * Uses JUnit 5 + Mockito as per project standards.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequest validRequest;
    private Customer existingCustomer;

    @BeforeEach
    void setUp() {
        validRequest = new CustomerRequest(
                "CUST001",
                "John Doe",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                new BigDecimal("5000.00"),
                CustomerStatus.ACTIVE,
                List.of(
                        new CustomerRequest.PhoneRequest(PhoneType.HOME, "555-1234"),
                        new CustomerRequest.PhoneRequest(PhoneType.MOBILE, "555-5678")
                )
        );

        existingCustomer = Customer.builder()
                .customerId("CUST001")
                .customerName("John Doe")
                .customerAddress("123 Main St")
                .customerCity("New York")
                .customerState("NY")
                .customerZip("10001")
                .customerBalance(new BigDecimal("5000.00"))
                .customerStatus(CustomerStatus.ACTIVE)
                .phones(List.of(
                        new CustomerPhone(PhoneType.HOME, "555-1234"),
                        new CustomerPhone(PhoneType.MOBILE, "555-5678")
                ))
                .lastUpdateDate(LocalDate.now())
                .build();
    }

    /**
     * Tests for createCustomer (1000-ADD-CUSTOMER paragraph)
     */
    @Nested
    @DisplayName("Create Customer (1000-ADD-CUSTOMER)")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully when customer does not exist")
        void createCustomer_Success() {
            // Given
            when(customerRepository.existsByCustomerId("CUST001")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(validRequest);

            // Then
            assertThat(response.customerId()).isEqualTo("CUST001");
            assertThat(response.customerName()).isEqualTo("John Doe");
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(response.customerStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(response.phones()).hasSize(2);

            verify(customerRepository).existsByCustomerId("CUST001");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw CustomerAlreadyExistsException when customer exists (COBOL code 1002)")
        void createCustomer_AlreadyExists() {
            // Given
            when(customerRepository.existsByCustomerId("CUST001")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessageContaining("CUST001");

            verify(customerRepository).existsByCustomerId("CUST001");
            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CustomerPersistenceException when save fails (COBOL code 1003)")
        void createCustomer_PersistenceFailed() {
            // Given
            when(customerRepository.existsByCustomerId("CUST001")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenThrow(new RuntimeException("DB error"));

            // When/Then
            assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                    .isInstanceOf(CustomerPersistenceException.class)
                    .hasMessageContaining("WRITE failed");

            verify(customerRepository).save(any(Customer.class));
        }
    }

    /**
     * Tests for updateCustomer (2000-UPDATE-CUSTOMER paragraph)
     */
    @Nested
    @DisplayName("Update Customer (2000-UPDATE-CUSTOMER)")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer successfully when customer exists")
        void updateCustomer_Success() {
            // Given
            CustomerRequest updateRequest = new CustomerRequest(
                    "CUST001",
                    "John Updated",
                    "456 Oak Ave",
                    "Los Angeles",
                    "CA",
                    "90001",
                    new BigDecimal("7500.00"),
                    CustomerStatus.ACTIVE,
                    List.of(new CustomerRequest.PhoneRequest(PhoneType.WORK, "555-9999"))
            );

            when(customerRepository.findById("CUST001")).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.updateCustomer("CUST001", updateRequest);

            // Then
            assertThat(response.customerName()).isEqualTo("John Updated");
            assertThat(response.customerCity()).isEqualTo("Los Angeles");
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal("7500.00"));

            verify(customerRepository).findById("CUST001");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found (COBOL code 2001)")
        void updateCustomer_NotFound() {
            // Given
            when(customerRepository.findById("CUST999")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customerService.updateCustomer("CUST999", validRequest))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("CUST999");

            verify(customerRepository).findById("CUST999");
            verify(customerRepository, never()).save(any());
        }
    }

    /**
     * Tests for deleteCustomer (3000-DELETE-CUSTOMER paragraph)
     */
    @Nested
    @DisplayName("Delete Customer (3000-DELETE-CUSTOMER)")
    class DeleteCustomerTests {

        @Test
        @DisplayName("Should delete customer successfully when customer exists")
        void deleteCustomer_Success() {
            // Given
            when(customerRepository.existsByCustomerId("CUST001")).thenReturn(true);
            doNothing().when(customerRepository).deleteById("CUST001");

            // When
            customerService.deleteCustomer("CUST001");

            // Then
            verify(customerRepository).existsByCustomerId("CUST001");
            verify(customerRepository).deleteById("CUST001");
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found (COBOL code 3001)")
        void deleteCustomer_NotFound() {
            // Given
            when(customerRepository.existsByCustomerId("CUST999")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> customerService.deleteCustomer("CUST999"))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("CUST999");

            verify(customerRepository).existsByCustomerId("CUST999");
            verify(customerRepository, never()).deleteById(any());
        }
    }

    /**
     * Tests for getCustomer (4000-INQUIRE-CUSTOMER paragraph)
     */
    @Nested
    @DisplayName("Get Customer (4000-INQUIRE-CUSTOMER)")
    class GetCustomerTests {

        @Test
        @DisplayName("Should return customer when customer exists")
        void getCustomer_Success() {
            // Given
            when(customerRepository.findById("CUST001")).thenReturn(Optional.of(existingCustomer));

            // When
            CustomerResponse response = customerService.getCustomer("CUST001");

            // Then
            assertThat(response.customerId()).isEqualTo("CUST001");
            assertThat(response.customerName()).isEqualTo("John Doe");
            assertThat(response.customerAddress()).isEqualTo("123 Main St");
            assertThat(response.customerCity()).isEqualTo("New York");
            assertThat(response.customerState()).isEqualTo("NY");
            assertThat(response.customerZip()).isEqualTo("10001");
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(response.customerStatus()).isEqualTo(CustomerStatus.ACTIVE);

            verify(customerRepository).findById("CUST001");
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found (COBOL code 4001)")
        void getCustomer_NotFound() {
            // Given
            when(customerRepository.findById("CUST999")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> customerService.getCustomer("CUST999"))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("CUST999");

            verify(customerRepository).findById("CUST999");
        }
    }

    /**
     * Tests for discount calculation (6000-APPLY-DISCOUNT paragraph)
     * COBOL logic: IF CUST-BALANCE > 10000.00, apply 5% discount
     */
    @Nested
    @DisplayName("Discount Calculation (6000-APPLY-DISCOUNT)")
    class DiscountCalculationTests {

        @Test
        @DisplayName("Should apply 5% discount when balance > 10000.00")
        void applyDiscount_AboveThreshold() {
            // Given - balance of 15000.00, expect 5% discount = 750.00
            CustomerRequest highBalanceRequest = new CustomerRequest(
                    "CUST002",
                    "Rich Customer",
                    "789 Luxury Lane",
                    "Beverly Hills",
                    "CA",
                    "90210",
                    new BigDecimal("15000.00"),
                    CustomerStatus.ACTIVE,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST002")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(highBalanceRequest);

            // Then - 15000.00 - (15000.00 * 0.05) = 15000.00 - 750.00 = 14250.00
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal("14250.00"));
        }

        @Test
        @DisplayName("Should NOT apply discount when balance <= 10000.00")
        void applyDiscount_BelowThreshold() {
            // Given - balance of 10000.00 exactly, no discount
            CustomerRequest exactThresholdRequest = new CustomerRequest(
                    "CUST003",
                    "Threshold Customer",
                    "100 Boundary St",
                    "Edge City",
                    "TX",
                    "75001",
                    new BigDecimal("10000.00"),
                    CustomerStatus.ACTIVE,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST003")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(exactThresholdRequest);

            // Then - no discount applied
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        }

        @Test
        @DisplayName("Should NOT apply discount when balance is below threshold")
        void applyDiscount_WellBelowThreshold() {
            // Given - balance of 5000.00, no discount
            when(customerRepository.existsByCustomerId("CUST001")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(validRequest);

            // Then - no discount applied
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @ParameterizedTest(name = "Balance {0} should result in {1} after discount")
        @DisplayName("Should calculate discount correctly for various balances")
        @CsvSource({
                "10000.01, 9500.01",   // Just above threshold, 5% off
                "20000.00, 19000.00",  // Round number
                "15500.50, 14725.48",  // With cents
                "100000.00, 95000.00", // Large balance
                "10001.00, 9500.95"    // Minimal over threshold
        })
        void applyDiscount_VariousBalances(String inputBalance, String expectedBalance) {
            // Given
            CustomerRequest request = new CustomerRequest(
                    "CUST999",
                    "Test Customer",
                    "Test Address",
                    "Test City",
                    "TX",
                    "12345",
                    new BigDecimal(inputBalance),
                    CustomerStatus.ACTIVE,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST999")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(request);

            // Then
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal(expectedBalance));
        }
    }

        /**
         * Explicit boundary test for COBOL's strict greater-than threshold.
         *
         * COBOL source (6000-APPLY-DISCOUNT):
         *   IF CUST-BALANCE > 10000.00   ← strictly greater-than, NOT >=
         *
         * Three critical boundary values:
         *   9999.99  — below  → no discount
         *   10000.00 — equal  → no discount (COBOL > is NOT >=)
         *   10000.01 — above  → 5% discount applied
         */
        @ParameterizedTest(name = "[{index}] balance={0} → expected={1} (discountApplied={2})")
        @DisplayName("COBOL boundary: IF CUST-BALANCE > 10000.00 (strict greater-than)")
        @CsvSource({
            "9999.99,  9999.99,  false",  // below threshold — no discount
            "10000.00, 10000.00, false",  // equal to threshold — COBOL > is NOT >=
            "10000.01, 9500.01,  true"    // above threshold — 5% discount: 10000.01 * 0.95 = 9500.0095 → 9500.01
        })
        void applyDiscount_CobolBoundary(String inputBalance, String expectedBalance,
                                         boolean discountApplied) {
            // Given
            CustomerRequest request = new CustomerRequest(
                    "CUST_BND",
                    "Boundary Test Customer",
                    "1 Threshold Lane",
                    "Boundary City",
                    "TX",
                    "75001",
                    new BigDecimal(inputBalance.trim()),
                    CustomerStatus.ACTIVE,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST_BND")).thenReturn(false);
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(request);

            // Then
            assertThat(response.customerBalance())
                    .as("Balance %s should %s have discount applied",
                        inputBalance, discountApplied ? "" : "NOT")
                    .isEqualByComparingTo(new BigDecimal(expectedBalance.trim()));
        }
    }

    /**
     * Tests for BigDecimal precision (COMP-3 packed decimal handling)
     */
    @Nested
    @DisplayName("BigDecimal Precision (COMP-3)")
    class BigDecimalPrecisionTests {

        @Test
        @DisplayName("Should preserve COMP-3 precision with 2 decimal places")
        void preservePrecision_TwoDecimals() {
            // Given - PIC S9(7)V99 means max 7 integer digits, 2 decimal
            CustomerRequest preciseRequest = new CustomerRequest(
                    "CUST100",
                    "Precise Customer",
                    "123 Decimal Dr",
                    "Precision City",
                    "CA",
                    "90001",
                    new BigDecimal("1234567.89"),
                    CustomerStatus.ACTIVE,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST100")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(preciseRequest);

            // Then
            assertThat(response.customerBalance().scale()).isLessThanOrEqualTo(2);
            assertThat(response.customerBalance().precision()).isLessThanOrEqualTo(9);
        }

        @Test
        @DisplayName("Should handle negative balance (signed COMP-3)")
        void handleNegativeBalance() {
            // Given - PIC S9(7)V99 supports signed values
            CustomerRequest negativeRequest = new CustomerRequest(
                    "CUST200",
                    "Negative Balance Customer",
                    "123 Debt Ave",
                    "Overdraft City",
                    "NY",
                    "10001",
                    new BigDecimal("-500.00"),
                    CustomerStatus.SUSPENDED,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST200")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(negativeRequest);

            // Then
            assertThat(response.customerBalance()).isNegative();
            assertThat(response.customerBalance()).isEqualByComparingTo(new BigDecimal("-500.00"));
        }

        @Test
        @DisplayName("Should handle zero balance")
        void handleZeroBalance() {
            // Given
            CustomerRequest zeroRequest = new CustomerRequest(
                    "CUST300",
                    "Zero Balance Customer",
                    "000 Empty St",
                    "Zero City",
                    "CA",
                    "00000",
                    BigDecimal.ZERO,
                    CustomerStatus.ACTIVE,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST300")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(zeroRequest);

            // Then
            assertThat(response.customerBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle null balance as zero")
        void handleNullBalance() {
            // Given
            CustomerRequest nullBalanceRequest = new CustomerRequest(
                    "CUST400",
                    "Null Balance Customer",
                    "Null St",
                    "Null City",
                    "CA",
                    "12345",
                    null,
                    CustomerStatus.ACTIVE,
                    null
            );

            when(customerRepository.existsByCustomerId("CUST400")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CustomerResponse response = customerService.createCustomer(nullBalanceRequest);

            // Then
            assertThat(response.customerBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    /**
     * Tests for getAllCustomers
     */
    @Nested
    @DisplayName("Get All Customers")
    class GetAllCustomersTests {

        @Test
        @DisplayName("Should return all customers")
        void getAllCustomers_Success() {
            // Given
            Customer customer2 = Customer.builder()
                    .customerId("CUST002")
                    .customerName("Jane Smith")
                    .customerBalance(new BigDecimal("2500.00"))
                    .customerStatus(CustomerStatus.ACTIVE)
                    .phones(List.of())
                    .build();

            when(customerRepository.findAll()).thenReturn(List.of(existingCustomer, customer2));

            // When
            List<CustomerResponse> responses = customerService.getAllCustomers();

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(CustomerResponse::customerId)
                    .containsExactly("CUST001", "CUST002");

            verify(customerRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no customers")
        void getAllCustomers_Empty() {
            // Given
            when(customerRepository.findAll()).thenReturn(List.of());

            // When
            List<CustomerResponse> responses = customerService.getAllCustomers();

            // Then
            assertThat(responses).isEmpty();

            verify(customerRepository).findAll();
        }
    }
}
