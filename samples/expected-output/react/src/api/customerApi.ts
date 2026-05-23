/**
 * Customer API client functions
 * Provides type-safe API calls for customer endpoints
 */

import type { Customer, CustomerInquiryResponse, ApiError } from '../types/customer';

/**
 * Base API URL - configure via environment variable
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * Custom error class for API errors
 */
export class CustomerApiError extends Error {
  public code: string;
  public details?: Record<string, string>;

  constructor(error: ApiError) {
    super(error.message);
    this.name = 'CustomerApiError';
    this.code = error.code;
    this.details = error.details;
  }
}

/**
 * Fetch wrapper with error handling and JSON parsing
 */
async function fetchWithErrorHandling<T>(
  url: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({
      code: 'UNKNOWN_ERROR',
      message: `HTTP ${response.status}: ${response.statusText}`,
    }));

    throw new CustomerApiError(errorBody as ApiError);
  }

  return response.json() as Promise<T>;
}

/**
 * Fetch customer by ID
 * Maps to mainframe CICS transaction: CINQ (Customer Inquiry)
 * 
 * @param customerId - The customer ID to look up (max 10 digits)
 * @returns Promise resolving to Customer data
 * @throws CustomerApiError if customer not found or API error
 */
export async function fetchCustomerById(customerId: string): Promise<Customer> {
  const response = await fetchWithErrorHandling<CustomerInquiryResponse>(
    `${API_BASE_URL}/customers/${encodeURIComponent(customerId)}`
  );

  if (!response.success || !response.data) {
    throw new CustomerApiError(
      response.error || {
        code: 'CUSTOMER_NOT_FOUND',
        message: `Customer ${customerId} not found`,
      }
    );
  }

  return response.data;
}

/**
 * Search customers by partial criteria
 * 
 * @param criteria - Search criteria (name, city, etc.)
 * @returns Promise resolving to array of matching customers
 */
export async function searchCustomers(criteria: {
  name?: string;
  city?: string;
  state?: string;
  status?: string;
}): Promise<Customer[]> {
  const params = new URLSearchParams();
  
  Object.entries(criteria).forEach(([key, value]) => {
    if (value) {
      params.append(key, value);
    }
  });

  return fetchWithErrorHandling<Customer[]>(
    `${API_BASE_URL}/customers?${params.toString()}`
  );
}

/**
 * Update customer information
 * Maps to mainframe CICS transaction: CUPD (Customer Update)
 * 
 * @param customerId - The customer ID to update
 * @param updates - Partial customer data to update
 * @returns Promise resolving to updated Customer data
 */
export async function updateCustomer(
  customerId: string,
  updates: Partial<Omit<Customer, 'id'>>
): Promise<Customer> {
  const response = await fetchWithErrorHandling<CustomerInquiryResponse>(
    `${API_BASE_URL}/customers/${encodeURIComponent(customerId)}`,
    {
      method: 'PUT',
      body: JSON.stringify(updates),
    }
  );

  if (!response.success || !response.data) {
    throw new CustomerApiError(
      response.error || {
        code: 'UPDATE_FAILED',
        message: `Failed to update customer ${customerId}`,
      }
    );
  }

  return response.data;
}

/**
 * Create a new customer
 * 
 * @param customerData - Customer data for the new record
 * @returns Promise resolving to created Customer with assigned ID
 */
export async function createCustomer(
  customerData: Omit<Customer, 'id' | 'lastUpdated'>
): Promise<Customer> {
  const response = await fetchWithErrorHandling<CustomerInquiryResponse>(
    `${API_BASE_URL}/customers`,
    {
      method: 'POST',
      body: JSON.stringify(customerData),
    }
  );

  if (!response.success || !response.data) {
    throw new CustomerApiError(
      response.error || {
        code: 'CREATE_FAILED',
        message: 'Failed to create customer',
      }
    );
  }

  return response.data;
}

/**
 * Delete a customer
 * Maps to mainframe CICS transaction: CDEL (Customer Delete)
 * 
 * @param customerId - The customer ID to delete
 * @returns Promise resolving to void on success
 */
export async function deleteCustomer(customerId: string): Promise<void> {
  await fetchWithErrorHandling<{ success: boolean }>(
    `${API_BASE_URL}/customers/${encodeURIComponent(customerId)}`,
    {
      method: 'DELETE',
    }
  );
}
