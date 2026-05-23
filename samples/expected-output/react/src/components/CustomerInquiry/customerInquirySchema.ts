/**
 * Zod validation schema for Customer Inquiry form
 * Derived from CUSTINQ BMS field definitions
 */

import { z } from 'zod';

/**
 * Customer ID validation schema
 * Based on BMS: CUSTID DFHMDF POS=(4,15),LENGTH=10,ATTRB=(UNPROT,NUM,FSET)
 * - UNPROT: User can modify
 * - NUM: Numeric only
 * - LENGTH=10: Maximum 10 characters
 */
export const customerIdSchema = z
  .string()
  .min(1, 'Customer ID is required')
  .max(10, 'Customer ID must be 10 digits or less')
  .regex(/^\d+$/, 'Customer ID must contain only numbers');

/**
 * Customer name schema (display field, but useful for validation if editable)
 * Based on BMS: CUSTNM DFHMDF POS=(6,15),LENGTH=30,ATTRB=(ASKIP)
 */
export const customerNameSchema = z
  .string()
  .max(30, 'Name must be 30 characters or less')
  .optional();

/**
 * Address schema
 * Based on BMS: CUSTAD DFHMDF POS=(8,15),LENGTH=40,ATTRB=(ASKIP)
 */
export const addressSchema = z
  .string()
  .max(40, 'Address must be 40 characters or less')
  .optional();

/**
 * City schema
 * Based on BMS: CUSTCY DFHMDF POS=(10,15),LENGTH=20,ATTRB=(ASKIP)
 */
export const citySchema = z
  .string()
  .max(20, 'City must be 20 characters or less')
  .optional();

/**
 * State schema
 * Based on BMS: CUSTST DFHMDF POS=(10,47),LENGTH=2,ATTRB=(ASKIP)
 */
export const stateSchema = z
  .string()
  .length(2, 'State must be exactly 2 characters')
  .regex(/^[A-Z]{2}$/, 'State must be 2 uppercase letters')
  .optional();

/**
 * ZIP code schema
 * Based on BMS: CUSTZIP DFHMDF POS=(10,60),LENGTH=10,ATTRB=(ASKIP)
 */
export const zipCodeSchema = z
  .string()
  .max(10, 'ZIP code must be 10 characters or less')
  .regex(/^\d{5}(-\d{4})?$/, 'Invalid ZIP code format')
  .optional();

/**
 * Customer inquiry form schema
 * Validates the input field (CUSTID) for the inquiry form
 */
export const customerInquiryFormSchema = z.object({
  customerId: customerIdSchema,
});

/**
 * Full customer data schema for API responses
 */
export const customerDataSchema = z.object({
  id: customerIdSchema,
  name: z.string().max(30),
  address: z.object({
    street: z.string().max(40),
    city: z.string().max(20),
    state: z.string().length(2),
    zipCode: z.string().max(10),
  }),
  balance: z.number(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED']),
  phones: z.array(z.object({
    type: z.enum(['home', 'work', 'mobile']),
    number: z.string(),
    isPrimary: z.boolean(),
  })).optional(),
  lastUpdated: z.string().optional(),
});

/**
 * Inferred TypeScript type from the form schema
 */
export type CustomerInquiryFormData = z.infer<typeof customerInquiryFormSchema>;

/**
 * Inferred TypeScript type from the customer data schema
 */
export type CustomerData = z.infer<typeof customerDataSchema>;
