import { type ReactNode } from 'react';
import { cn } from '../../lib/utils';

type PageGuidanceTone = 'parent' | 'child';

type PageGuidanceProps = {
  title: string;
  description: string;
  instructions: string;
  tone: PageGuidanceTone;
  icon?: ReactNode;
};

/**
 * Renders a tone-aware page introduction block for parent/child contexts.
 *
 * @param param0 Guidance title, supporting description, usage instructions, and tone.
 * @returns A guidance section with semantic heading and accessible label.
 * @example
 * <PageGuidance title="Your Reading List 📚" description="Track progress." instructions="Check off chapters." tone="child" />
 */
export function PageGuidance({ title, description, instructions, tone, icon }: PageGuidanceProps) {
  const parentTone = tone === 'parent';

  return (
    <section
      aria-label={`${title} page guidance`}
      className={cn(
        'mb-6 rounded-xl border p-4 shadow-sm sm:p-6',
        parentTone ? 'border-blue-200 bg-blue-50' : 'border-amber-200 bg-amber-50'
      )}
    >
      <div className="flex items-start gap-3">
        {icon ? (
          <div className={cn('mt-1 text-2xl', parentTone ? 'text-blue-700' : 'text-amber-700')} aria-hidden="true">
            {icon}
          </div>
        ) : null}
        <div>
          <h1 className={cn('m-0 text-2xl font-bold sm:text-3xl', parentTone ? 'text-blue-900' : 'text-amber-900')}>
            {title}
          </h1>
          <p className="mb-2 mt-2 text-sm leading-relaxed text-text-primary sm:text-base">{description}</p>
          <p className="m-0 text-sm italic leading-relaxed text-text-secondary">{instructions}</p>
        </div>
      </div>
    </section>
  );
}
