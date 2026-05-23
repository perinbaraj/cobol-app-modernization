/**
 * Custom hook for Customer Inquiry using React Query
 * Provides data fetching, caching, and state management
 */

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchCustomerById, CustomerApiError } from '../../api/customerApi';
import type { Customer } from '../../types/customer';

/**
 * Query key factory for customer queries
 * Enables precise cache invalidation
 */
export const customerQueryKeys = {
  all: ['customers'] as const,
  detail: (id: string) => [...customerQueryKeys.all, 'detail', id] as const,
};

/**
 * Return type for useCustomerInquiry hook
 */
export interface UseCustomerInquiryResult {
  /** Customer data if available */
  data: Customer | undefined;
  /** Loading state */
  isLoading: boolean;
  /** Fetching state (true during refetch) */
  isFetching: boolean;
  /** Error object if query failed */
  error: CustomerApiError | null;
  /** Whether query has been executed at least once */
  isInitialLoading: boolean;
  /** Whether the query is in error state */
  isError: boolean;
  /** Whether the query has succeeded */
  isSuccess: boolean;
  /** Manually trigger a refetch */
  refetch: () => void;
  /** Clear the current query data */
  clearData: () => void;
}

/**
 * Hook options
 */
export interface UseCustomerInquiryOptions {
  /** Whether to enable the query (useful for conditional fetching) */
  enabled?: boolean;
  /** Stale time in milliseconds (default: 30 seconds) */
  staleTime?: number;
  /** Cache time in milliseconds (default: 5 minutes) */
  gcTime?: number;
  /** Whether to refetch on window focus (default: false) */
  refetchOnWindowFocus?: boolean;
  /** Retry count on failure (default: 1) */
  retry?: number;
}

/**
 * Custom hook for customer inquiry
 * Fetches customer data by ID using React Query
 * 
 * @param customerId - The customer ID to look up (optional)
 * @param options - Query configuration options
 * @returns Query result with data, loading, error states and refetch function
 * 
 * @example
 * ```tsx
 * const { data, isLoading, error, refetch } = useCustomerInquiry('1234567890');
 * 
 * if (isLoading) return <LoadingSpinner />;
 * if (error) return <ErrorMessage error={error} />;
 * if (data) return <CustomerDetails customer={data} />;
 * ```
 */
export function useCustomerInquiry(
  customerId: string | undefined,
  options: UseCustomerInquiryOptions = {}
): UseCustomerInquiryResult {
  const queryClient = useQueryClient();

  const {
    enabled = true,
    staleTime = 30 * 1000, // 30 seconds
    gcTime = 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus = false,
    retry = 1,
  } = options;

  const query = useQuery<Customer, CustomerApiError>({
    queryKey: customerQueryKeys.detail(customerId ?? ''),
    queryFn: () => {
      if (!customerId) {
        throw new CustomerApiError({
          code: 'INVALID_ID',
          message: 'Customer ID is required',
        });
      }
      return fetchCustomerById(customerId);
    },
    enabled: enabled && !!customerId && customerId.length > 0,
    staleTime,
    gcTime,
    refetchOnWindowFocus,
    retry,
  });

  /**
   * Manually trigger a refetch of the customer data
   */
  const refetch = () => {
    if (customerId) {
      query.refetch();
    }
  };

  /**
   * Clear the current query data from cache
   */
  const clearData = () => {
    if (customerId) {
      queryClient.removeQueries({
        queryKey: customerQueryKeys.detail(customerId),
      });
    }
  };

  return {
    data: query.data,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error: query.error,
    isInitialLoading: query.isLoading && query.isFetching,
    isError: query.isError,
    isSuccess: query.isSuccess,
    refetch,
    clearData,
  };
}
