import { type ReactNode } from 'react';
import { Modal as UIModal } from '../ui/modal';

type ModalProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
};

/**
 * Shared modal wrapper for dialog-based flows.
 *
 * @param props Modal state, heading content, body content, and optional footer.
 * @returns A modal dialog component.
 * @example
 * <Modal open={open} onOpenChange={setOpen} title="Reset Password">
 *   <ResetForm />
 * </Modal>
 */
export function Modal(props: ModalProps) {
  return <UIModal {...props} />;
}
