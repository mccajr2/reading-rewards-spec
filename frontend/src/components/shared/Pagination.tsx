import { type ComponentPropsWithoutRef } from 'react';
import { Pagination as UIPagination } from '../ui/pagination';

type PaginationProps = ComponentPropsWithoutRef<typeof UIPagination>;

/**
 * Shared pagination wrapper around the UI pagination primitive.
 *
 * @param props Pagination state and callback props.
 * @returns A pagination navigation component.
 * @example
 * <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
 */
export function Pagination(props: PaginationProps) {
  return <UIPagination {...props} />;
}
