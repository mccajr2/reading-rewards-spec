import { render, screen } from '@testing-library/react';
import { Button } from '../Button';
import { Card, CardContent, CardTitle } from '../Card';
import { Input } from '../Input';

describe('Shared primitives', () => {
  it('renders button variant and size classes', () => {
    render(
      <Button variant="secondary" size="lg">
        Click Me
      </Button>
    );

    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toHaveClass('min-h-11');
    expect(button).toHaveClass('h-12');
  });

  it('renders card content with design-system classes', () => {
    render(
      <Card>
        <CardTitle>Summary</CardTitle>
        <CardContent>Card content</CardContent>
      </Card>
    );

    const title = screen.getByRole('heading', { name: /summary/i });
    const wrapper = title.closest('div');
    expect(wrapper).not.toBeNull();
    expect(screen.getByText(/card content/i)).toBeInTheDocument();
  });

  it('renders input with focus and accessibility classes', () => {
    render(<Input placeholder="Email" />);

    const input = screen.getByPlaceholderText(/email/i);
    expect(input).toHaveClass('h-11');
    expect(input).toHaveClass('focus-visible:ring-2');
  });
});
