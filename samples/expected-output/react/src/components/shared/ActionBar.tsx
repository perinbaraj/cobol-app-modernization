/**
 * ActionBar component - PF Key equivalent action bar
 * Converts mainframe PF key navigation to modern keyboard shortcuts and buttons
 * 
 * Original BMS: PFKEYS DFHMDF POS=(24,1),LENGTH=60,ATTRB=(ASKIP),
 *               INITIAL='PF1=HELP  PF3=EXIT  PF5=REFRESH  ENTER=SEARCH'
 */

import React, { useCallback } from 'react';
import { useHotkeys } from 'react-hotkeys-hook';

/**
 * Action definition for the action bar
 */
export interface Action {
  /** Unique identifier for the action */
  id: string;
  /** Display label */
  label: string;
  /** Keyboard shortcut (e.g., 'F1', 'Enter', 'Escape') */
  shortcut: string;
  /** Callback function when action is triggered */
  onAction: () => void;
  /** Whether the action is disabled */
  disabled?: boolean;
  /** Button variant */
  variant?: 'primary' | 'secondary' | 'danger';
  /** Aria label for accessibility */
  ariaLabel?: string;
}

/**
 * Standard PF key to keyboard shortcut mapping
 */
export const PF_KEY_MAP = {
  PF1: 'F1',
  PF2: 'F2',
  PF3: 'Escape', // F3=Exit commonly mapped to Escape
  PF4: 'F4',
  PF5: 'F5', // Refresh
  PF6: 'F6',
  PF7: 'PageUp',
  PF8: 'PageDown',
  PF9: 'F9',
  PF10: 'F10',
  PF11: 'F11',
  PF12: 'F12',
  ENTER: 'Enter',
  CLEAR: 'Escape',
} as const;

export interface ActionBarProps {
  /** Array of actions to display */
  actions: Action[];
  /** Additional CSS classes */
  className?: string;
  /** Position of the action bar */
  position?: 'top' | 'bottom';
  /** Show keyboard shortcuts in buttons */
  showShortcuts?: boolean;
}

/**
 * ActionBar component
 * Provides keyboard shortcut navigation equivalent to mainframe PF keys
 */
export function ActionBar({
  actions,
  className = '',
  position = 'bottom',
  showShortcuts = true,
}: ActionBarProps) {
  // Register all keyboard shortcuts
  const enabledActions = actions.filter((a) => !a.disabled);
  
  // Build hotkeys string (comma-separated shortcuts)
  const hotkeysString = enabledActions
    .map((a) => a.shortcut.toLowerCase())
    .join(', ');

  const handleHotkey = useCallback(
    (event: KeyboardEvent) => {
      const key = event.key;
      const action = enabledActions.find(
        (a) => a.shortcut.toLowerCase() === key.toLowerCase()
      );
      
      if (action) {
        event.preventDefault();
        action.onAction();
      }
    },
    [enabledActions]
  );

  useHotkeys(hotkeysString, handleHotkey, {
    enableOnFormTags: ['INPUT', 'SELECT', 'TEXTAREA'],
    preventDefault: true,
  });

  /**
   * Get button styling based on variant
   */
  const getButtonStyles = (variant: Action['variant'] = 'secondary') => {
    const baseStyles =
      'px-4 py-2 rounded-md font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';
    
    switch (variant) {
      case 'primary':
        return `${baseStyles} bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500`;
      case 'danger':
        return `${baseStyles} bg-red-600 text-white hover:bg-red-700 focus:ring-red-500`;
      case 'secondary':
      default:
        return `${baseStyles} bg-gray-200 text-gray-800 hover:bg-gray-300 focus:ring-gray-500 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600`;
    }
  };

  /**
   * Format shortcut for display
   */
  const formatShortcut = (shortcut: string) => {
    const shortcuts: Record<string, string> = {
      enter: '↵',
      escape: 'Esc',
      pageup: 'PgUp',
      pagedown: 'PgDn',
    };
    return shortcuts[shortcut.toLowerCase()] || shortcut;
  };

  const positionStyles = position === 'top' 
    ? 'border-b border-gray-200 dark:border-gray-700' 
    : 'border-t border-gray-200 dark:border-gray-700';

  return (
    <div
      className={`flex items-center justify-between px-4 py-3 bg-gray-100 dark:bg-gray-800 ${positionStyles} ${className}`}
      role="toolbar"
      aria-label="Action bar"
    >
      <div className="flex items-center gap-2">
        {actions.map((action) => (
          <button
            key={action.id}
            type="button"
            onClick={action.onAction}
            disabled={action.disabled}
            className={getButtonStyles(action.variant)}
            aria-label={action.ariaLabel || action.label}
            aria-keyshortcuts={action.shortcut}
          >
            {action.label}
            {showShortcuts && (
              <kbd className="ml-2 px-1.5 py-0.5 text-xs bg-gray-300 dark:bg-gray-600 rounded">
                {formatShortcut(action.shortcut)}
              </kbd>
            )}
          </button>
        ))}
      </div>
      
      {/* Keyboard shortcut hint */}
      <div className="hidden md:flex items-center text-sm text-gray-500 dark:text-gray-400">
        <span className="mr-2">Keyboard shortcuts:</span>
        {enabledActions.slice(0, 4).map((action, index) => (
          <React.Fragment key={action.id}>
            <kbd className="px-1.5 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded">
              {formatShortcut(action.shortcut)}
            </kbd>
            <span className="mx-1">
              {action.label.split('=')[0] || action.label}
            </span>
            {index < Math.min(enabledActions.length, 4) - 1 && (
              <span className="mx-2 text-gray-300 dark:text-gray-600">|</span>
            )}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}

/**
 * Pre-configured action bar for Customer Inquiry screen
 * Matches BMS: PF1=HELP  PF3=EXIT  PF5=REFRESH  ENTER=SEARCH
 */
export interface CustomerInquiryActionBarProps {
  onHelp: () => void;
  onExit: () => void;
  onRefresh: () => void;
  onSearch: () => void;
  isLoading?: boolean;
}

export function CustomerInquiryActionBar({
  onHelp,
  onExit,
  onRefresh,
  onSearch,
  isLoading = false,
}: CustomerInquiryActionBarProps) {
  const actions: Action[] = [
    {
      id: 'search',
      label: 'Search',
      shortcut: 'Enter',
      onAction: onSearch,
      variant: 'primary',
      disabled: isLoading,
      ariaLabel: 'Search for customer (Enter)',
    },
    {
      id: 'refresh',
      label: 'Refresh',
      shortcut: 'F5',
      onAction: onRefresh,
      variant: 'secondary',
      disabled: isLoading,
      ariaLabel: 'Refresh data (F5)',
    },
    {
      id: 'help',
      label: 'Help',
      shortcut: 'F1',
      onAction: onHelp,
      variant: 'secondary',
      ariaLabel: 'Show help (F1)',
    },
    {
      id: 'exit',
      label: 'Exit',
      shortcut: 'Escape',
      onAction: onExit,
      variant: 'secondary',
      ariaLabel: 'Exit screen (Escape)',
    },
  ];

  return <ActionBar actions={actions} />;
}
