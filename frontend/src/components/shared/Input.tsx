import { forwardRef, type ComponentPropsWithoutRef } from 'react';
import { Input as UIInput } from '../ui/input';

type InputProps = ComponentPropsWithoutRef<typeof UIInput>;

/**
 * Shared input wrapper with forwarded ref support.
 *
 * @param props Input attributes and design-system class overrides.
 * @param ref Forwarded ref to the underlying input element.
 * @returns A styled input field.
 * @example
 * <Input type="email" placeholder="you@example.com" />
 */
export const Input = forwardRef<HTMLInputElement, InputProps>((props, ref) => {
  return <UIInput ref={ref} {...props} />;
});

Input.displayName = 'Input';
