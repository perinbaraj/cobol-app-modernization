/**
 * CustomerInquiry component
 * Converted from CUSTINQ BMS screen map to React 18.2 + TypeScript + Tailwind CSS
 * 
 * Original BMS: CUSTINQ DFHMSD TYPE=MAP,MODE=INOUT,LANG=COBOL,STORAGE=AUTO
 * Map: CUSTMAP DFHMDI SIZE=(24,80),LINE=1,COLUMN=1
 */

import React, { useState, useCallback, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';

import { customerInquiryFormSchema, type CustomerInquiryFormData } from './customerInquirySchema';
import { useCustomerInquiry } from './useCustomerInquiry';
import { CustomerInquiryActionBar } from '../shared/ActionBar';
import { MessageArea, useMessage } from '../shared/MessageArea';
import type { Customer } from '../../types/customer';

/**
 * Props for CustomerInquiry component
 */
export interface CustomerInquiryProps {
  /** Initial customer ID to look up (optional) */
  initialCustomerId?: string;
  /** Callback when user exits the screen */
  onExit?: () => void;
  /** Callback when customer data is loaded */
  onCustomerLoaded?: (customer: Customer) => void;
}

/**
 * CustomerInquiry component
 * Modern React implementation of mainframe Customer Inquiry screen
 */
export function CustomerInquiry({
  initialCustomerId = '',
  onExit,
  onCustomerLoaded,
}: CustomerInquiryProps) {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const { message, showError, showSuccess, showInfo, clearMessage } = useMessage();
  
  // State for the customer ID to query
  const [queryCustomerId, setQueryCustomerId] = useState<string>(initialCustomerId);

  // React Hook Form setup with Zod validation
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CustomerInquiryFormData>({
    resolver: zodResolver(customerInquiryFormSchema),
    defaultValues: {
      customerId: initialCustomerId,
    },
  });

  // Customer data query
  const { data: customer, isLoading, isFetching, error, refetch } = useCustomerInquiry(
    queryCustomerId,
    { enabled: queryCustomerId.length > 0 }
  );

  // Handle successful data load
  useEffect(() => {
    if (customer) {
      showSuccess(`Customer ${customer.id} found: ${customer.name}`);
      onCustomerLoaded?.(customer);
    }
  }, [customer, showSuccess, onCustomerLoaded]);

  // Handle errors
  useEffect(() => {
    if (error) {
      showError(error.message, error.code);
    }
  }, [error, showError]);

  // Focus input on mount
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // Set initial customer ID if provided
  useEffect(() => {
    if (initialCustomerId) {
      setValue('customerId', initialCustomerId);
      setQueryCustomerId(initialCustomerId);
    }
  }, [initialCustomerId, setValue]);

  /**
   * Handle form submission - Search action (ENTER key equivalent)
   */
  const onSubmit = useCallback((data: CustomerInquiryFormData) => {
    clearMessage();
    setQueryCustomerId(data.customerId);
  }, [clearMessage]);

  /**
   * Handle refresh action (PF5 equivalent)
   */
  const handleRefresh = useCallback(() => {
    if (queryCustomerId) {
      clearMessage();
      refetch();
    } else {
      showInfo('Enter a Customer ID first');
    }
  }, [queryCustomerId, clearMessage, refetch, showInfo]);

  /**
   * Handle help action (PF1 equivalent)
   */
  const handleHelp = useCallback(() => {
    showInfo('Customer Inquiry: Enter a customer ID and press Enter to search. Use F5 to refresh, Escape to exit.');
  }, [showInfo]);

  /**
   * Handle exit action (PF3 equivalent)
   */
  const handleExit = useCallback(() => {
    if (onExit) {
      onExit();
    } else {
      navigate(-1);
    }
  }, [onExit, navigate]);

  /**
   * Format currency for display
   */
  const formatCurrency = (value: number | undefined): string => {
    if (value === undefined) return '—';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);
  };

  /**
   * Format address for display
   */
  const formatAddress = (customer: Customer | undefined): string => {
    if (!customer?.address) return '—';
    const { street, city, state, zipCode } = customer.address;
    if (!street && !city && !state && !zipCode) return '—';
    return [street, city, state, zipCode].filter(Boolean).join(', ');
  };

  const isLoadingState = isLoading || isFetching || isSubmitting;

  return (
    <div className="min-h-screen bg-white dark:bg-gray-900 flex flex-col">
      {/* Screen Title - Maps to BMS TITLE field with BRT attribute */}
      <header className="bg-gray-100 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 py-4">
        <h1 className="text-2xl font-bold text-center text-gray-900 dark:text-white">
          Customer Inquiry
        </h1>
      </header>

      <main className="flex-1 p-6">
        <div className="max-w-2xl mx-auto">
          {/* Customer ID Input Section */}
          <form onSubmit={handleSubmit(onSubmit)} className="mb-8">
            <div className="flex items-center gap-4">
              {/* CIDLBL - Customer ID Label (ASKIP) */}
              <label
                htmlFor="customerId"
                className="text-sm font-medium text-gray-700 dark:text-gray-300 w-28"
              >
                Customer ID:
              </label>
              
              {/* CUSTID - Customer ID Input (UNPROT,NUM) */}
              <input
                {...register('customerId')}
                ref={(e) => {
                  register('customerId').ref(e);
                  (inputRef as React.MutableRefObject<HTMLInputElement | null>).current = e;
                }}
                id="customerId"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={10}
                autoComplete="off"
                aria-label="Customer ID - numeric, maximum 10 digits"
                aria-invalid={!!errors.customerId}
                aria-describedby={errors.customerId ? 'customerId-error' : undefined}
                disabled={isLoadingState}
                className={`
                  w-40 px-3 py-2 rounded-md border
                  text-gray-900 dark:text-white
                  bg-white dark:bg-gray-800
                  focus:outline-none focus:ring-2 focus:ring-blue-500
                  disabled:bg-gray-100 dark:disabled:bg-gray-700 disabled:cursor-not-allowed
                  ${errors.customerId 
                    ? 'border-red-500 focus:ring-red-500' 
                    : 'border-gray-300 dark:border-gray-600'
                  }
                `}
                placeholder="Enter ID"
              />
              
              <button
                type="submit"
                disabled={isLoadingState}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoadingState ? 'Searching...' : 'Search'}
              </button>
            </div>
            
            {errors.customerId && (
              <p id="customerId-error" className="mt-2 text-sm text-red-600 dark:text-red-400 ml-32">
                {errors.customerId.message}
              </p>
            )}
          </form>

          {/* Loading State */}
          {isLoadingState && (
            <div className="flex items-center justify-center py-8" role="status" aria-live="polite">
              <svg
                className="animate-spin h-8 w-8 text-blue-600"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              <span className="ml-3 text-gray-600 dark:text-gray-400">Loading customer data...</span>
            </div>
          )}

          {/* Customer Display Fields - All ASKIP fields */}
          {customer && !isLoadingState && (
            <div
              className="bg-gray-50 dark:bg-gray-800 rounded-lg p-6 border border-gray-200 dark:border-gray-700"
              aria-label="Customer Details"
            >
              <dl className="grid grid-cols-[120px_1fr] gap-y-4 gap-x-4">
                {/* NAMLBL/CUSTNM - Customer Name */}
                <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Name:
                </dt>
                <dd className="text-sm text-gray-900 dark:text-white">
                  {customer.name || '—'}
                </dd>

                {/* ADRLBL/CUSTAD - Address */}
                <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Address:
                </dt>
                <dd className="text-sm text-gray-900 dark:text-white">
                  {customer.address?.street || '—'}
                </dd>

                {/* CTYLBL/CUSTCY - City, STLBL/CUSTST - State, ZIPLBL/CUSTZIP - ZIP */}
                <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  City/State/ZIP:
                </dt>
                <dd className="text-sm text-gray-900 dark:text-white">
                  {customer.address 
                    ? `${customer.address.city || ''}, ${customer.address.state || ''} ${customer.address.zipCode || ''}`
                    : '—'
                  }
                </dd>

                {/* BALLBL/CUSTBAL - Balance (BRT attribute = font-bold) */}
                <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Balance:
                </dt>
                <dd className="text-sm font-bold text-gray-900 dark:text-white">
                  {formatCurrency(customer.balance)}
                </dd>

                {/* STSLBL/CUSTSTS - Status */}
                <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Status:
                </dt>
                <dd className="text-sm text-gray-900 dark:text-white">
                  <span
                    className={`
                      inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                      ${customer.status === 'ACTIVE' 
                        ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' 
                        : customer.status === 'SUSPENDED'
                        ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
                        : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200'
                      }
                    `}
                  >
                    {customer.status || '—'}
                  </span>
                </dd>
              </dl>
            </div>
          )}

          {/* Empty State */}
          {!customer && !isLoadingState && !error && queryCustomerId === '' && (
            <div className="text-center py-12 text-gray-500 dark:text-gray-400">
              <p>Enter a Customer ID and press Enter or click Search to view customer details.</p>
            </div>
          )}
        </div>
      </main>

      {/* Message Area - Maps to BMS MSG field */}
      <div className="px-6 pb-4">
        <MessageArea
          message={message}
          onDismiss={clearMessage}
          className="max-w-2xl mx-auto"
        />
      </div>

      {/* Action Bar - Maps to BMS PFKEYS field */}
      <CustomerInquiryActionBar
        onSearch={handleSubmit(onSubmit)}
        onRefresh={handleRefresh}
        onHelp={handleHelp}
        onExit={handleExit}
        isLoading={isLoadingState}
      />
    </div>
  );
}

export default CustomerInquiry;
