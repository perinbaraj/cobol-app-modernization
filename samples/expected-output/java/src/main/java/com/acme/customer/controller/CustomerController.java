package com.acme.customer.controller;

import com.acme.customer.dto.CustomerRequest;
import com.acme.customer.dto.CustomerResponse;
import com.acme.customer.exception.CustomerException;
import com.acme.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer REST controller.
 * Exposes customer CRUD operations as REST endpoints.
 * Migrated from CUSTMGMT.cbl COBOL program transaction types:
 * - 'A' (Add)    → POST /customers
 * - 'I' (Inquiry) → GET /customers/{id}
 * - 'U' (Update) → PUT /customers/{id}
 * - 'D' (Delete) → DELETE /customers/{id}
 */
@RestController
@RequestMapping("/customers")
@Tag(name = "Customer", description = "Customer management API - migrated from CUSTMGMT.cbl")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Create a new customer.
     * COBOL equivalent: Transaction type 'A' (1000-ADD-CUSTOMER paragraph)
     *
     * @param request the customer request
     * @return the created customer
     */
    @PostMapping
    @Operation(summary = "Create a new customer",
               description = "Creates a new customer record. Equivalent to COBOL ADD transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Customer already exists (COBOL code 1002)")
    })
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a customer by ID.
     * COBOL equivalent: Transaction type 'I' (4000-INQUIRE-CUSTOMER paragraph)
     *
     * @param id the customer ID
     * @return the customer
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID",
               description = "Retrieves a customer by their ID. Equivalent to COBOL INQUIRY transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer found",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found (COBOL code 4001)")
    })
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer ID (max 10 characters)")
            @PathVariable("id") String id) {
        CustomerResponse response = customerService.getCustomer(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all customers.
     *
     * @return list of all customers
     */
    @GetMapping
    @Operation(summary = "Get all customers",
               description = "Retrieves all customer records.")
    @ApiResponse(responseCode = "200", description = "List of customers",
            content = @Content(schema = @Schema(implementation = CustomerResponse.class)))
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> responses = customerService.getAllCustomers();
        return ResponseEntity.ok(responses);
    }

    /**
     * Update an existing customer.
     * COBOL equivalent: Transaction type 'U' (2000-UPDATE-CUSTOMER paragraph)
     *
     * @param id      the customer ID
     * @param request the customer request
     * @return the updated customer
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer",
               description = "Updates an existing customer record. Equivalent to COBOL UPDATE transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Customer not found (COBOL code 2001)")
    })
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer ID (max 10 characters)")
            @PathVariable("id") String id,
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a customer.
     * COBOL equivalent: Transaction type 'D' (3000-DELETE-CUSTOMER paragraph)
     *
     * @param id the customer ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer",
               description = "Deletes a customer record. Equivalent to COBOL DELETE transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found (COBOL code 3001)")
    })
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Customer ID (max 10 characters)")
            @PathVariable("id") String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Global exception handler for CustomerException hierarchy.
     * Maps COBOL error codes to HTTP status codes.
     */
    @ExceptionHandler(CustomerException.class)
    public ResponseEntity<ErrorResponse> handleCustomerException(CustomerException ex) {
        ErrorResponse error = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * Error response DTO.
     *
     * @param errorCode COBOL-equivalent error code
     * @param message   Error message
     */
    public record ErrorResponse(int errorCode, String message) {
    }
}
