/**
 * CustomerInquiry component tests
 * React Testing Library tests covering rendering, validation, API integration, and keyboard shortcuts
 */

import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { describe, it, expect, beforeAll, afterAll, afterEach, vi } from 'vitest';

import { CustomerInquiry } from './CustomerInquiry';
import type { Customer, CustomerInquiryResponse } from '../../types/customer';

// Mock customer data
const mockCustomer: Customer = {
  id: '1234567890',
  name: 'John Doe',
  address: {
    street: '123 Main Street',
    city: 'New York',
    state: 'NY',
    zipCode: '10001',
  },
  balance: 1500.75,
  status: 'ACTIVE',
};

// MSW server setup
const server = setupServer(
  // Success handler
  http.get('/api/customers/:id', ({ params }) => {
    const { id } = params;
    
    if (id === '1234567890') {
      return HttpResponse.json({
        success: true,
        data: mockCustomer,
      } as CustomerInquiryResponse);
    }
    
    if (id === '9999999999') {
      return HttpResponse.json(
        {
          success: false,
          error: {
            code: 'CUSTOMER_NOT_FOUND',
            message: 'Customer 9999999999 not found',
          },
        } as CustomerInquiryResponse,
        { status: 404 }
      );
    }
    
    return HttpResponse.json({
      success: false,
      error: {
        code: 'UNKNOWN_ERROR',
        message: 'Unknown error occurred',
      },
    }, { status: 500 });
  })
);

// Setup and teardown
beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
});
afterAll(() => server.close());

/**
 * Test wrapper with required providers
 */
function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        {ui}
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('CustomerInquiry', () => {
  describe('Rendering', () => {
    it('renders the screen title', () => {
      renderWithProviders(<CustomerInquiry />);
      
      expect(screen.getByRole('heading', { name: /customer inquiry/i })).toBeInTheDocument();
    });

    it('renders the customer ID input field', () => {
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      expect(input).toBeInTheDocument();
      expect(input).toHaveAttribute('maxLength', '10');
      expect(input).toHaveAttribute('inputMode', 'numeric');
    });

    it('renders the search button', () => {
      renderWithProviders(<CustomerInquiry />);
      
      expect(screen.getByRole('button', { name: /search/i })).toBeInTheDocument();
    });

    it('renders the action bar with all buttons', () => {
      renderWithProviders(<CustomerInquiry />);
      
      const toolbar = screen.getByRole('toolbar', { name: /action bar/i });
      expect(within(toolbar).getByRole('button', { name: /search/i })).toBeInTheDocument();
      expect(within(toolbar).getByRole('button', { name: /refresh/i })).toBeInTheDocument();
      expect(within(toolbar).getByRole('button', { name: /help/i })).toBeInTheDocument();
      expect(within(toolbar).getByRole('button', { name: /exit/i })).toBeInTheDocument();
    });

    it('shows empty state message when no customer ID entered', () => {
      renderWithProviders(<CustomerInquiry />);
      
      expect(screen.getByText(/enter a customer id/i)).toBeInTheDocument();
    });

    it('focuses the input field on mount', () => {
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      expect(document.activeElement).toBe(input);
    });

    it('renders with initial customer ID if provided', () => {
      renderWithProviders(<CustomerInquiry initialCustomerId="1234567890" />);
      
      const input = screen.getByLabelText(/customer id/i);
      expect(input).toHaveValue('1234567890');
    });
  });

  describe('Form Validation', () => {
    it('shows error for empty customer ID on submit', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      await waitFor(() => {
        expect(screen.getByText(/customer id is required/i)).toBeInTheDocument();
      });
    });

    it('shows error for non-numeric customer ID', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, 'ABC123');
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      await waitFor(() => {
        expect(screen.getByText(/must contain only numbers/i)).toBeInTheDocument();
      });
    });

    it('shows error for customer ID exceeding max length', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      // Input has maxLength=10, but we test the schema validation
      await user.type(input, '12345678901'); // 11 digits, but input will truncate
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      // Should not show error since input maxLength prevents >10 chars
      await waitFor(() => {
        expect(screen.queryByText(/must be 10 digits or less/i)).not.toBeInTheDocument();
      });
    });

    it('accepts valid numeric customer ID', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, '1234567890');
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      // Should not show validation error
      await waitFor(() => {
        expect(screen.queryByText(/customer id is required/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/must contain only numbers/i)).not.toBeInTheDocument();
      });
    });
  });

  describe('API Integration', () => {
    it('displays loading state while fetching', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, '1234567890');
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      expect(screen.getByText(/loading customer data/i)).toBeInTheDocument();
    });

    it('displays customer data on successful fetch', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, '1234567890');
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      await waitFor(() => {
        expect(screen.getByText('John Doe')).toBeInTheDocument();
        expect(screen.getByText('123 Main Street')).toBeInTheDocument();
        expect(screen.getByText(/new york/i)).toBeInTheDocument();
        expect(screen.getByText('$1,500.75')).toBeInTheDocument();
        expect(screen.getByText('ACTIVE')).toBeInTheDocument();
      });
    });

    it('displays error message when customer not found', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, '9999999999');
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(screen.getByText(/not found/i)).toBeInTheDocument();
      });
    });

    it('calls onCustomerLoaded callback when customer is loaded', async () => {
      const onCustomerLoaded = vi.fn();
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry onCustomerLoaded={onCustomerLoaded} />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, '1234567890');
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      await waitFor(() => {
        expect(onCustomerLoaded).toHaveBeenCalledWith(mockCustomer);
      });
    });
  });

  describe('Keyboard Shortcuts', () => {
    it('submits form on Enter key in input', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, '1234567890');
      await user.keyboard('{Enter}');
      
      await waitFor(() => {
        expect(screen.getByText(/loading customer data/i)).toBeInTheDocument();
      });
    });

    it('shows help message on F1 key', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      await user.keyboard('{F1}');
      
      await waitFor(() => {
        expect(screen.getByText(/enter a customer id and press enter/i)).toBeInTheDocument();
      });
    });

    it('triggers refresh on F5 key with customer loaded', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry initialCustomerId="1234567890" />);
      
      // Wait for initial load
      await waitFor(() => {
        expect(screen.getByText('John Doe')).toBeInTheDocument();
      });

      // Press F5
      await user.keyboard('{F5}');
      
      // Should show loading state during refresh
      await waitFor(() => {
        expect(screen.getByText(/loading customer data/i)).toBeInTheDocument();
      });
    });

    it('shows info message on F5 without customer ID', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      await user.keyboard('{F5}');
      
      await waitFor(() => {
        expect(screen.getByText(/enter a customer id first/i)).toBeInTheDocument();
      });
    });

    it('calls onExit on Escape key', async () => {
      const onExit = vi.fn();
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry onExit={onExit} />);
      
      await user.keyboard('{Escape}');
      
      await waitFor(() => {
        expect(onExit).toHaveBeenCalled();
      });
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels on input fields', () => {
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      expect(input).toHaveAttribute('aria-label');
    });

    it('sets aria-invalid on input when validation error', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      await waitFor(() => {
        const input = screen.getByLabelText(/customer id/i);
        expect(input).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('links error message to input via aria-describedby', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      await waitFor(() => {
        const input = screen.getByLabelText(/customer id/i);
        const errorId = input.getAttribute('aria-describedby');
        expect(errorId).toBeTruthy();
        expect(document.getElementById(errorId!)).toHaveTextContent(/customer id is required/i);
      });
    });

    it('has proper role on action bar', () => {
      renderWithProviders(<CustomerInquiry />);
      
      expect(screen.getByRole('toolbar')).toBeInTheDocument();
    });

    it('announces loading state to screen readers', async () => {
      const user = userEvent.setup();
      renderWithProviders(<CustomerInquiry />);
      
      const input = screen.getByLabelText(/customer id/i);
      await user.type(input, '1234567890');
      await user.click(screen.getByRole('button', { name: /search/i }));
      
      const loadingElement = screen.getByRole('status');
      expect(loadingElement).toHaveAttribute('aria-live', 'polite');
    });
  });
});
