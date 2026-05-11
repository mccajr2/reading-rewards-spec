import { Button } from './button';
import { cn } from '../../lib/utils';

type PaginationProps = {
  page: number;
  totalPages: number;
  onPageChange: (nextPage: number) => void;
  className?: string;
};

/**
 * Pagination control with previous/next and page number buttons.
 *
 * @param param0 Current page, total pages, and page-change callback.
 * @returns A pagination navigation region.
 * @example
 * <Pagination page={1} totalPages={5} onPageChange={setPage} />
 */
export function Pagination({ page, totalPages, onPageChange, className }: PaginationProps) {
  const pages = Array.from({ length: totalPages }, (_, idx) => idx + 1);

  return (
    <nav aria-label="Pagination" className={cn('flex items-center gap-2', className)}>
      <Button variant="secondary" size="sm" onClick={() => onPageChange(page - 1)} disabled={page <= 1}>
        Previous
      </Button>
      <div className="flex items-center gap-1">
        {pages.map((candidate) => (
          <Button
            key={candidate}
            variant={candidate === page ? 'default' : 'outline'}
            size="sm"
            onClick={() => onPageChange(candidate)}
            aria-current={candidate === page ? 'page' : undefined}
          >
            {candidate}
          </Button>
        ))}
      </div>
      <Button
        variant="secondary"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages}
      >
        Next
      </Button>
    </nav>
  );
}
