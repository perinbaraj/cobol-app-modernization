/**
 * TypeScript interfaces for Customer domain
 * Derived from CUSTINQ BMS screen map fields
 */

/**
 * Customer phone information
 */
export interface CustomerPhone {
  type: 'home' | 'work' | 'mobile';
  number: string;
  isPrimary: boolean;
}

/**
 * Customer address information
 * Maps to BMS fields: CUSTAD, CUSTCY, CUSTST, CUSTZIP
 */
export interface CustomerAddress {
  street: string;    // CUSTAD - LENGTH=40
  city: string;      // CUSTCY - LENGTH=20
  state: string;     // CUSTST - LENGTH=2
  zipCode: string;   // CUSTZIP - LENGTH=10
}

/**
 * Customer status enumeration
 * Maps to BMS field: CUSTSTS
 */
export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';

/**
 * Main Customer interface
 * Maps to CUSTINQ BMS map fields
 */
export interface Customer {
  /** Customer ID - CUSTID field, numeric, max 10 digits */
  id: string;
  
  /** Customer name - CUSTNM field, max 30 characters */
  name: string;
  
  /** Customer address */
  address: CustomerAddress;
  
  /** Account balance - CUSTBAL field, displayed with BRT attribute */
  balance: number;
  
  /** Account status - CUSTSTS field */
  status: CustomerStatus;
  
  /** Phone numbers (not in original BMS but common addition) */
  phones?: CustomerPhone[];
  
  /** Last updated timestamp */
  lastUpdated?: string;
}

/**
 * Customer inquiry request
 */
export interface CustomerInquiryRequest {
  customerId: string;
}

/**
 * Customer inquiry response from API
 */
export interface CustomerInquiryResponse {
  success: boolean;
  data?: Customer;
  error?: {
    code: string;
    message: string;
  };
}

/**
 * API error response structure
 */
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, string>;
}
