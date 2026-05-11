import { useMemo, useRef, useState, type KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import { Button } from './Button';
import { cn } from '../../lib/utils';

export type NavigationItem = {
  label: string;
  to: string;
  visible?: boolean;
};

type NavigationProps = {
  brand: string;
  items: NavigationItem[];
  activePath: string;
  creditsText?: string;
  userText?: string;
  onLogout: () => void;
};

/**
 * Top navigation with keyboard arrow support and role-aware link visibility.
 *
 * @param param0 Navigation labels, active route, optional user/credit badges, and logout handler.
 * @returns A responsive navigation bar.
 * @example
 * <Navigation brand="Reading Rewards" items={items} activePath={pathname} onLogout={logout} />
 */
export function Navigation({ brand, items, activePath, creditsText, userText, onLogout }: NavigationProps) {
  const visibleItems = useMemo(() => items.filter((item) => item.visible ?? true), [items]);
  const [focusedIndex, setFocusedIndex] = useState<number>(0);
  const linkRefs = useRef<Array<HTMLAnchorElement | null>>([]);

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (visibleItems.length === 0) return;

    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;

    event.preventDefault();
    const nextIndex =
      event.key === 'ArrowRight'
        ? (focusedIndex + 1) % visibleItems.length
        : (focusedIndex - 1 + visibleItems.length) % visibleItems.length;

    setFocusedIndex(nextIndex);
    linkRefs.current[nextIndex]?.focus();
  };

  return (
    <nav className="border-b border-border bg-background px-4 py-3 sm:px-6" aria-label="Primary navigation">
      <div className="mx-auto flex w-full max-w-[1100px] flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex w-full flex-wrap items-center gap-2 sm:w-auto" role="menubar" onKeyDown={onKeyDown}>
          <span className="mr-2 text-base font-bold text-primary sm:text-lg">{brand}</span>
          {visibleItems.map((item, index) => {
            const isActive = activePath === item.to;
            return (
              <Link
                key={item.to}
                ref={(node) => {
                  linkRefs.current[index] = node;
                }}
                role="menuitem"
                tabIndex={index === focusedIndex ? 0 : -1}
                to={item.to}
                className={cn(
                  'rounded-md px-3 py-2 text-sm font-medium outline-none ring-primary transition-colors focus-visible:ring-2',
                  isActive ? 'bg-primary text-white' : 'text-text-secondary hover:bg-background-alt hover:text-text-primary'
                )}
                aria-current={isActive ? 'page' : undefined}
              >
                {item.label}
              </Link>
            );
          })}
        </div>

        <div className="flex w-full flex-wrap items-center justify-between gap-2 sm:w-auto sm:justify-end">
          {creditsText ? (
            <span className="rounded-full bg-success px-3 py-1 text-xs font-bold text-text-primary sm:text-sm">{creditsText}</span>
          ) : null}
          {userText ? <span className="text-xs text-text-secondary sm:text-sm">{userText}</span> : null}
          <Button variant="secondary" size="sm" onClick={onLogout}>
            Logout
          </Button>
        </div>
      </div>
    </nav>
  );
}
