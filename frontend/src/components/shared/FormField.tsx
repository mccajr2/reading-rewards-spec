import { type InputHTMLAttributes, useId } from 'react';
import { Label } from '../ui/label';
import { Input } from './Input';

type FormFieldProps = {
  label: string;
  error?: string;
  helperText?: string;
} & InputHTMLAttributes<HTMLInputElement>;

/**
 * Composes a labeled text input with helper and error messaging.
 *
 * @param param0 FormField props including label text, optional helper/error, and input attributes.
 * @returns A field block with connected label and ARIA-described helper/error content.
 * @example
 * <FormField label="Username" name="username" helperText="3-20 characters" />
 */
export function FormField({ label, error, helperText, id, ...inputProps }: FormFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;
  const helpId = `${inputId}-help`;

  const describedBy = [error ? errorId : null, helperText ? helpId : null].filter(Boolean).join(' ') || undefined;

  return (
    <div className="grid gap-2">
      <Label htmlFor={inputId}>{label}</Label>
      <Input id={inputId} aria-invalid={Boolean(error)} aria-describedby={describedBy} {...inputProps} />
      {error ? (
        <p id={errorId} className="m-0 text-sm font-medium text-error">
          {error}
        </p>
      ) : null}
      {!error && helperText ? (
        <p id={helpId} className="m-0 text-sm text-text-secondary">
          {helperText}
        </p>
      ) : null}
    </div>
  );
}
