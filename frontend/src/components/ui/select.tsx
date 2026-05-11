import * as React from 'react';
import { cn } from '../../lib/utils';

export type SelectOption = {
  value: string;
  label: string;
};

type SelectProps = Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'children'> & {
  options: SelectOption[];
  placeholder?: string;
};

/**
 * Native select wrapper with options array support.
 *
 * @param param0 Select props including options and optional placeholder.
 * @param ref Forwarded select reference.
 * @returns A styled select control.
 * @example
 * <Select options={[{ value: 'child', label: 'Child' }]} value={role} onChange={...} />
 */
export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, options, placeholder, ...props }, ref) => {
    return (
      <select
        ref={ref}
        className={cn(
          'h-11 w-full rounded-md border border-border bg-background px-3 text-sm text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary',
          className
        )}
        {...props}
      >
        {placeholder ? (
          <option value="" disabled>
            {placeholder}
          </option>
        ) : null}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    );
  }
);

Select.displayName = 'Select';
