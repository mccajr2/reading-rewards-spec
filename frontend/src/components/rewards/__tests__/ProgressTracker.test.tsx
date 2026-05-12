import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { ProgressTracker } from '../ProgressTracker';

describe('ProgressTracker', () => {
  it('requires chapter tracking when reward is per chapter', () => {
    render(
      <ProgressTracker
        rewardUnit="PER_CHAPTER"
        initialTrackingType="NONE"
        onSave={vi.fn()}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /check completion requirements/i }));
    expect(screen.getByRole('alert')).toHaveTextContent(/chapter count required to complete this book/i);
  });

  it('submits chapter and page values for save', async () => {
    const onSave = vi.fn();

    render(
      <ProgressTracker
        rewardUnit="PER_BOOK"
        initialTrackingType="CHAPTERS"
        onSave={onSave}
      />
    );

    fireEvent.change(screen.getByLabelText(/total chapters/i), { target: { value: '12' } });
    fireEvent.change(screen.getByLabelText(/current chapter/i), { target: { value: '3' } });
    fireEvent.click(screen.getByRole('button', { name: /save progress/i }));

    expect(onSave).toHaveBeenCalledWith({
      trackingType: 'CHAPTERS',
      totalChapters: 12,
      currentChapter: 3,
      totalPages: null,
      currentPage: null,
    });
  });
});
