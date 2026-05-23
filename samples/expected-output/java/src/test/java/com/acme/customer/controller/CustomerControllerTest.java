package com.acme.customer.controller;

import com.acme.customer.dto.CustomerRequest;
import com.acme.customer.dto.CustomerResponse;
import com.acme.customer.exception.CustomerException.*;
import com.acme.customer.model.CustomerStatus;
import com.acme.customer.model.PhoneType;
import com.acme.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc integration tests for CustomerController.
 * Tests REST endpoints that map to COBOL transaction types.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    private CustomerRequest validRequest;
    private CustomerResponse validResponse;

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

        validResponse = new CustomerResponse(
                "CUST001",
                "John Doe",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                new BigDecimal("5000.00"),
                CustomerStatus.ACTIVE,
                LocalDate.now(),
                List.of(
                        new CustomerResponse.PhoneResponse(PhoneType.HOME, "555-1234"),
                        new CustomerResponse.PhoneResponse(PhoneType.MOBILE, "555-5678")
                )
        );
    }

    /**
     * Tests for POST /customers (Create - COBOL transaction 'A')
     */
    @Nested
    @DisplayName("POST /customers (Add Customer)")
    class CreateCustomerEndpointTests {

        @Test
        @DisplayName("Should create customer and return 201 Created")
        void createCustomer_Success() throws Exception {
            // Given
            when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(validResponse);

            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.customerId").value("CUST001"))
                    .andExpect(jsonPath("$.customerName").value("John Doe"))
                    .andExpect(jsonPath("$.customerBalance").value(5000.00))
                    .andExpect(jsonPath("$.customerStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.phones", hasSize(2)));

            verify(customerService).createCustomer(any(CustomerRequest.class));
        }

        @Test
        @DisplayName("Should return 409 Conflict when customer already exists (COBOL code 1002)")
        void createCustomer_AlreadyExists() throws Exception {
            // Given
            when(customerService.createCustomer(any(CustomerRequest.class)))
                    .thenThrow(new CustomerAlreadyExistsException("CUST001"));

            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value(1002))
                    .andExpect(jsonPath("$.message").value(containsString("CUST001")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for invalid request data")
        void createCustomer_InvalidRequest() throws Exception {
            // Given - missing required fields
            CustomerRequest invalidRequest = new CustomerRequest(
                    "",  // Invalid: blank customer ID
                    "",  // Invalid: blank name
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,  // Invalid: null status
                    null
            );

            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when customer ID exceeds 10 characters")
        void createCustomer_IdTooLong() throws Exception {
            // Given - customer ID > 10 chars (PIC X(10))
            CustomerRequest longIdRequest = new CustomerRequest(
                    "CUSTOMERID12345",  // 15 characters, exceeds PIC X(10)
                    "Valid Name",
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.ZERO,
                    CustomerStatus.ACTIVE,
                    null
            );

            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(longIdRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * Tests for GET /customers/{id} (Inquiry - COBOL transaction 'I')
     */
    @Nested
    @DisplayName("GET /customers/{id} (Inquire Customer)")
    class GetCustomerEndpointTests {

        @Test
        @DisplayName("Should return customer when found")
        void getCustomer_Success() throws Exception {
            // Given
            when(customerService.getCustomer("CUST001")).thenReturn(validResponse);

            // When/Then
            mockMvc.perform(get("/customers/CUST001"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.customerId").value("CUST001"))
                    .andExpect(jsonPath("$.customerName").value("John Doe"))
                    .andExpect(jsonPath("$.customerAddress").value("123 Main St"))
                    .andExpect(jsonPath("$.customerCity").value("New York"))
                    .andExpect(jsonPath("$.customerState").value("NY"))
                    .andExpect(jsonPath("$.customerZip").value("10001"))
                    .andExpect(jsonPath("$.customerBalance").value(5000.00))
                    .andExpect(jsonPath("$.customerStatus").value("ACTIVE"));

            verify(customerService).getCustomer("CUST001");
        }

        @Test
        @DisplayName("Should return 404 Not Found when customer not found (COBOL code 4001)")
        void getCustomer_NotFound() throws Exception {
            // Given
            when(customerService.getCustomer("CUST999"))
                    .thenThrow(new CustomerNotFoundException("CUST999"));

            // When/Then
            mockMvc.perform(get("/customers/CUST999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(2001))
                    .andExpect(jsonPath("$.message").value(containsString("CUST999")));
        }
    }

    /**
     * Tests for GET /customers (List all)
     */
    @Nested
    @DisplayName("GET /customers (List All Customers)")
    class GetAllCustomersEndpointTests {

        @Test
        @DisplayName("Should return all customers")
        void getAllCustomers_Success() throws Exception {
            // Given
            CustomerResponse response2 = new CustomerResponse(
                    "CUST002",
                    "Jane Smith",
                    "456 Oak Ave",
                    "Los Angeles",
                    "CA",
                    "90001",
                    new BigDecimal("7500.00"),
                    CustomerStatus.ACTIVE,
                    LocalDate.now(),
                    List.of()
            );

            when(customerService.getAllCustomers()).thenReturn(List.of(validResponse, response2));

            // When/Then
            mockMvc.perform(get("/customers"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].customerId").value("CUST001"))
                    .andExpect(jsonPath("$[1].customerId").value("CUST002"));

            verify(customerService).getAllCustomers();
        }

        @Test
        @DisplayName("Should return empty list when no customers")
        void getAllCustomers_Empty() throws Exception {
            // Given
            when(customerService.getAllCustomers()).thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/customers"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    /**
     * Tests for PUT /customers/{id} (Update - COBOL transaction 'U')
     */
    @Nested
    @DisplayName("PUT /customers/{id} (Update Customer)")
    class UpdateCustomerEndpointTests {

        @Test
        @DisplayName("Should update customer and return 200 OK")
        void updateCustomer_Success() throws Exception {
            // Given
            CustomerResponse updatedResponse = new CustomerResponse(
                    "CUST001",
                    "John Updated",
                    "789 New Address",
                    "San Francisco",
                    "CA",
                    "94102",
                    new BigDecimal("8000.00"),
                    CustomerStatus.ACTIVE,
                    LocalDate.now(),
                    List.of()
            );

            when(customerService.updateCustomer(eq("CUST001"), any(CustomerRequest.class)))
                    .thenReturn(updatedResponse);

            // When/Then
            mockMvc.perform(put("/customers/CUST001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value("CUST001"))
                    .andExpect(jsonPath("$.customerName").value("John Updated"))
                    .andExpect(jsonPath("$.customerCity").value("San Francisco"));

            verify(customerService).updateCustomer(eq("CUST001"), any(CustomerRequest.class));
        }

        @Test
        @DisplayName("Should return 404 Not Found when customer not found (COBOL code 2001)")
        void updateCustomer_NotFound() throws Exception {
            // Given
            when(customerService.updateCustomer(eq("CUST999"), any(CustomerRequest.class)))
                    .thenThrow(new CustomerNotFoundException("CUST999"));

            // When/Then
            mockMvc.perform(put("/customers/CUST999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(2001));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for invalid update data")
        void updateCustomer_InvalidRequest() throws Exception {
            // Given - invalid request with blank required fields
            CustomerRequest invalidRequest = new CustomerRequest(
                    "",
                    "",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // When/Then
            mockMvc.perform(put("/customers/CUST001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * Tests for DELETE /customers/{id} (Delete - COBOL transaction 'D')
     */
    @Nested
    @DisplayName("DELETE /customers/{id} (Delete Customer)")
    class DeleteCustomerEndpointTests {

        @Test
        @DisplayName("Should delete customer and return 204 No Content")
        void deleteCustomer_Success() throws Exception {
            // Given
            doNothing().when(customerService).deleteCustomer("CUST001");

            // When/Then
            mockMvc.perform(delete("/customers/CUST001"))
                    .andExpect(status().isNoContent());

            verify(customerService).deleteCustomer("CUST001");
        }

        @Test
        @DisplayName("Should return 404 Not Found when customer not found (COBOL code 3001)")
        void deleteCustomer_NotFound() throws Exception {
            // Given
            doThrow(new CustomerNotFoundException("CUST999"))
                    .when(customerService).deleteCustomer("CUST999");

            // When/Then
            mockMvc.perform(delete("/customers/CUST999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(2001))
                    .andExpect(jsonPath("$.message").value(containsString("CUST999")));
        }
    }

    /**
     * Tests for discount calculation via API (COBOL 6000-APPLY-DISCOUNT)
     */
    @Nested
    @DisplayName("Discount Calculation via API")
    class DiscountEndpointTests {

        @Test
        @DisplayName("Should apply discount when creating customer with high balance")
        void createCustomer_WithDiscount() throws Exception {
            // Given - customer with balance > 10000 gets 5% discount
            CustomerRequest highBalanceRequest = new CustomerRequest(
                    "CUST100",
                    "Rich Customer",
                    "100 Luxury Lane",
                    "Beverly Hills",
                    "CA",
                    "90210",
                    new BigDecimal("15000.00"),
                    CustomerStatus.ACTIVE,
                    null
            );

            CustomerResponse discountedResponse = new CustomerResponse(
                    "CUST100",
                    "Rich Customer",
                    "100 Luxury Lane",
                    "Beverly Hills",
                    "CA",
                    "90210",
                    new BigDecimal("14250.00"),  // 15000 - 5% = 14250
                    CustomerStatus.ACTIVE,
                    LocalDate.now(),
                    List.of()
            );

            when(customerService.createCustomer(any(CustomerRequest.class)))
                    .thenReturn(discountedResponse);

            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(highBalanceRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerBalance").value(14250.00));
        }

        @Test
        @DisplayName("Should return 422 when discount calculation overflows (COBOL code 6001)")
        void createCustomer_DiscountOverflow() throws Exception {
            // Given
            when(customerService.createCustomer(any(CustomerRequest.class)))
                    .thenThrow(new DiscountCalculationException("DISCOUNT CALC OVERFLOW"));

            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode").value(6001))
                    .andExpect(jsonPath("$.message").value("DISCOUNT CALC OVERFLOW"));
        }
    }

    /**
     * Tests for content type handling
     */
    @Nested
    @DisplayName("Content Type Handling")
    class ContentTypeTests {

        @Test
        @DisplayName("Should reject non-JSON content type")
        void createCustomer_InvalidContentType() throws Exception {
            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("invalid"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should accept application/json content type")
        void createCustomer_ValidContentType() throws Exception {
            // Given
            when(customerService.createCustomer(any(CustomerRequest.class)))
                    .thenReturn(validResponse);

            // When/Then
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
