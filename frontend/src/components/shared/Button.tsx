import { type ComponentPropsWithoutRef } from 'react';
import { Button as UIButton } from '../ui/button';

type ButtonProps = ComponentPropsWithoutRef<typeof UIButton>;

/**
 * Shared button wrapper around the UI primitive button.
 *
 * @param props Button props including variant, size, and native button attributes.
 * @returns A styled button element with design-system defaults.
 * @example
 * <Button variant="secondary" size="sm">Cancel</Button>
 */
export function Button(props: ButtonProps) {
  return <UIButton {...props} />;
}
