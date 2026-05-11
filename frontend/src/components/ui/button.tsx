import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../../lib/utils';

const buttonVariants = cva(
  'inline-flex min-h-11 items-center justify-center rounded-md text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:pointer-events-none disabled:opacity-60',
  {
    variants: {
      variant: {
        default: 'bg-primary text-white hover:opacity-90',
        secondary: 'bg-background-alt text-text-primary border border-border hover:bg-primary-light',
        outline: 'border border-border bg-white text-text-primary hover:bg-background-alt',
        ghost: 'text-text-primary hover:bg-background-alt'
      },
      size: {
        sm: 'min-h-11 h-11 px-3',
        md: 'h-11 px-4',
        lg: 'h-12 px-5'
      }
    },
    defaultVariants: {
      variant: 'default',
      size: 'md'
    }
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

/**
 * Design-system button with variant and size options.
 *
 * @param param0 Button props including variant, size, and native attributes.
 * @param ref Forwarded ref to the button element.
 * @returns A styled button element.
 * @example
 * <Button variant="outline" size="sm">Back</Button>
 */
const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => {
    return <button className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />;
  }
);
Button.displayName = 'Button';

export { Button, buttonVariants };
