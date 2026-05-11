/**
 * Design-system theme tokens used by component styles and documentation.
 *
 * @example
 * import { theme } from './theme';
 *
 * const buttonStyle = {
 *   backgroundColor: theme.colors.primary,
 *   padding: `${theme.spacing.sm}px ${theme.spacing.base}px`,
 * };
 */
export const theme = {
  /** Semantic color tokens. */
  colors: {
    primary: '#2563eb',
    primaryLight: '#eff6ff',
    accent: '#fbbf24',
    accentLight: '#fffbeb',
    success: '#16a34a',
    warning: '#ea580c',
    error: '#dc2626',
    textPrimary: '#111827',
    textSecondary: '#4b5563',
    border: '#e5e7eb',
    background: '#ffffff',
    backgroundAlt: '#f9fafb'
  },
  /** Typography scale and font-weight/line-height tokens. */
  typography: {
    fontFamily: "system-ui, -apple-system, 'Segoe UI', sans-serif",
    fontSizes: {
      xs: 12,
      sm: 14,
      base: 16,
      lg: 18,
      xl: 20,
      '2xl': 24,
      '3xl': 28
    },
    lineHeights: {
      tight: 1.2,
      normal: 1.5,
      relaxed: 1.6
    },
    fontWeights: {
      regular: 400,
      medium: 500,
      semibold: 600,
      bold: 700
    }
  },
  /** Spacing scale in pixel values. */
  spacing: {
    xs: 4,
    sm: 8,
    md: 12,
    base: 16,
    lg: 24,
    xl: 32,
    '2xl': 48
  },
  /** Breakpoint values used in responsive behavior. */
  breakpoints: {
    mobile: '0px',
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px'
  },
  /** Shadow presets for elevation. */
  shadows: {
    sm: '0 1px 2px rgba(0,0,0,0.05)',
    base: '0 1px 3px rgba(0,0,0,0.1)',
    lg: '0 10px 15px rgba(0,0,0,0.1)'
  }
} as const;
