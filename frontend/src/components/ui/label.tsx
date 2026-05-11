import * as React from 'react';
import * as LabelPrimitive from '@radix-ui/react-label';
import { cn } from '../../lib/utils';

const Label = React.forwardRef<
  React.ElementRef<typeof LabelPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof LabelPrimitive.Root>
>(({ className, ...props }, ref) => (
  <LabelPrimitive.Root ref={ref} className={cn('text-sm font-medium text-text-primary', className)} {...props} />
));

Label.displayName = LabelPrimitive.Root.displayName;

/**
 * Form label component tied to control IDs.
 *
 * @param props Label attributes such as htmlFor and children.
 * @returns A styled label element.
 * @example
 * <Label htmlFor="email">Email</Label>
 */
export { Label };
