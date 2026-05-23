/**
 * MessageArea component - Message display matching BMS MSG field
 * 
 * Original BMS: MSG DFHMDF POS=(22,1),LENGTH=79,ATTRB=(ASKIP,BRT)
 * 
 * Supports info, warning, and error styling with appropriate accessibility
 */

import React from 'react';

/**
 * Message severity levels
 */
export type MessageSeverity = 'info' | 'success' | 'warning' | 'error';

/**
 * Message object structure
 */
export interface Message {
  /** Unique identifier for the message */
  id?: string;
  /** Message text content */
  text: string;
  /** Severity level determining styling */
  severity: MessageSeverity;
  /** Optional error code (useful for mainframe error mapping) */
  code?: string;
  /** Whether the message can be dismissed */
  dismissible?: boolean;
}

export interface MessageAreaProps {
  /** Message to display, or null/undefined to hide */
  message: Message | null | undefined;
  /** Callback when message is dismissed */
  onDismiss?: () => void;
  /** Additional CSS classes */
  className?: string;
  /** Aria live region politeness */
  ariaLive?: 'polite' | 'assertive' | 'off';
  /** Maximum length for display (matches BMS LENGTH=79) */
  maxLength?: number;
}

/**
 * MessageArea component
 * Displays system messages with appropriate styling and accessibility
 */
export function MessageArea({
  message,
  onDismiss,
  className = '',
  ariaLive = 'polite',
  maxLength = 79,
}: MessageAreaProps) {
  if (!message) {
    return null;
  }

  /**
   * Get styling based on message severity
   */
  const getSeverityStyles = (severity: MessageSeverity): string => {
    const baseStyles = 'px-4 py-3 rounded-md font-medium';
    
    switch (severity) {
      case 'success':
        return `${baseStyles} bg-green-100 text-green-800 border border-green-300 dark:bg-green-900 dark:text-green-200 dark:border-green-700`;
      case 'warning':
        return `${baseStyles} bg-yellow-100 text-yellow-800 border border-yellow-300 dark:bg-yellow-900 dark:text-yellow-200 dark:border-yellow-700`;
      case 'error':
        return `${baseStyles} bg-red-100 text-red-800 border border-red-300 dark:bg-red-900 dark:text-red-200 dark:border-red-700`;
      case 'info':
      default:
        return `${baseStyles} bg-blue-100 text-blue-800 border border-blue-300 dark:bg-blue-900 dark:text-blue-200 dark:border-blue-700`;
    }
  };

  /**
   * Get appropriate icon for severity
   */
  const getSeverityIcon = (severity: MessageSeverity): JSX.Element => {
    const iconClasses = 'w-5 h-5 flex-shrink-0';
    
    switch (severity) {
      case 'success':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        );
      case 'warning':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        );
      case 'error':
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
          </svg>
        );
      case 'info':
      default:
        return (
          <svg className={iconClasses} fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
        );
    }
  };

  /**
   * Get ARIA role for severity
   */
  const getAriaRole = (severity: MessageSeverity): string => {
    return severity === 'error' ? 'alert' : 'status';
  };

  /**
   * Truncate message if it exceeds maxLength
   */
  const truncatedText = message.text.length > maxLength
    ? `${message.text.substring(0, maxLength - 3)}...`
    : message.text;

  return (
    <div
      className={`${getSeverityStyles(message.severity)} ${className}`}
      role={getAriaRole(message.severity)}
      aria-live={ariaLive}
      aria-atomic="true"
    >
      <div className="flex items-start">
        {getSeverityIcon(message.severity)}
        
        <div className="ml-3 flex-1">
          {message.code && (
            <span className="font-mono text-sm mr-2">
              [{message.code}]
            </span>
          )}
          <span>{truncatedText}</span>
        </div>

        {message.dismissible && onDismiss && (
          <button
            type="button"
            onClick={onDismiss}
            className="ml-3 flex-shrink-0 rounded-md p-1.5 hover:bg-black/10 dark:hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-offset-2"
            aria-label="Dismiss message"
          >
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </button>
        )}
      </div>
    </div>
  );
}

/**
 * Hook for managing messages
 * Provides state management for message display
 */
export function useMessage() {
  const [message, setMessage] = React.useState<Message | null>(null);

  const showMessage = React.useCallback((msg: Message) => {
    setMessage({
      ...msg,
      id: msg.id || Date.now().toString(),
    });
  }, []);

  const showInfo = React.useCallback((text: string) => {
    showMessage({ text, severity: 'info' });
  }, [showMessage]);

  const showSuccess = React.useCallback((text: string) => {
    showMessage({ text, severity: 'success' });
  }, [showMessage]);

  const showWarning = React.useCallback((text: string) => {
    showMessage({ text, severity: 'warning' });
  }, [showMessage]);

  const showError = React.useCallback((text: string, code?: string) => {
    showMessage({ text, severity: 'error', code });
  }, [showMessage]);

  const clearMessage = React.useCallback(() => {
    setMessage(null);
  }, []);

  return {
    message,
    showMessage,
    showInfo,
    showSuccess,
    showWarning,
    showError,
    clearMessage,
  };
}
