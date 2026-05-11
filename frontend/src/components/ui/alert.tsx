import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../../lib/utils';

const alertVariants = cva('relative w-full rounded-lg border p-4 text-sm', {
  variants: {
    variant: {
      info: 'border-primary bg-primary-light text-text-primary',
      success: 'border-success/30 bg-green-50 text-green-900',
      warning: 'border-warning/30 bg-orange-50 text-orange-900',
      error: 'border-error/30 bg-red-50 text-red-900'
    }
  },
  defaultVariants: {
    variant: 'info'
  }
});

const Alert = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & VariantProps<typeof alertVariants>
>(({ className, variant, ...props }, ref) => (
  <div ref={ref} role="alert" className={cn(alertVariants({ variant }), className)} {...props} />
));
Alert.displayName = 'Alert';

const AlertTitle = React.forwardRef<HTMLHeadingElement, React.HTMLAttributes<HTMLHeadingElement>>(
  ({ className, ...props }, ref) => <h5 ref={ref} className={cn('mb-1 font-semibold', className)} {...props} />
);
AlertTitle.displayName = 'AlertTitle';

const AlertDescription = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLParagraphElement>>(
  ({ className, ...props }, ref) => <p ref={ref} className={cn('leading-relaxed', className)} {...props} />
);
AlertDescription.displayName = 'AlertDescription';

/**
 * Alert primitives for info/success/warning/error messaging.
 *
 * @param variant Visual state variant for the alert container.
 * @returns Alert container, title, and description primitives.
 * @example
 * <Alert variant="success"><AlertTitle>Saved</AlertTitle><AlertDescription>Done.</AlertDescription></Alert>
 */
export { Alert, AlertDescription, AlertTitle };
