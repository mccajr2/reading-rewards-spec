import { render, screen } from '@testing-library/react';
import { BookOpen } from 'lucide-react';
import { PageGuidance } from '../PageGuidance';

describe('PageGuidance', () => {
  it('renders title, description, and instructions', () => {
    render(
      <PageGuidance
        title="Your Dashboard"
        description="Track your child progress."
        instructions="Open a child card to view details."
        tone="parent"
      />
    );

    expect(screen.getByRole('heading', { name: /your dashboard/i })).toBeInTheDocument();
    expect(screen.getByText(/track your child progress/i)).toBeInTheDocument();
    expect(screen.getByText(/open a child card/i)).toBeInTheDocument();
  });

  it('applies parent tone styles', () => {
    const { container } = render(
      <PageGuidance
        title="Parent"
        description="Description"
        instructions="Instructions"
        tone="parent"
      />
    );

    expect(container.firstChild).toHaveClass('border-blue-200');
    expect(container.firstChild).toHaveClass('bg-blue-50');
  });

  it('applies child tone styles and renders optional icon', () => {
    const { container } = render(
      <PageGuidance
        title="Reading List"
        description="Fun description"
        instructions="Fun instructions"
        tone="child"
        icon={<BookOpen data-testid="guidance-icon" />}
      />
    );

    expect(container.firstChild).toHaveClass('border-amber-200');
    expect(container.firstChild).toHaveClass('bg-amber-50');
    expect(screen.getByTestId('guidance-icon')).toBeInTheDocument();
  });
});
