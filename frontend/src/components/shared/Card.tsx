import { type ComponentPropsWithoutRef } from 'react';
import {
  Card as UICard,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle
} from '../ui/card';

type CardProps = ComponentPropsWithoutRef<typeof UICard>;

/**
 * Shared card wrapper that re-exports card composition subcomponents.
 *
 * @param props Card container props such as className and children.
 * @returns A card container with design-system styling.
 * @example
 * <Card>
 *   <CardTitle>Summary</CardTitle>
 *   <CardContent>Content</CardContent>
 * </Card>
 */
export function Card(props: CardProps) {
  return <UICard {...props} />;
}

export { CardContent, CardDescription, CardFooter, CardHeader, CardTitle };
