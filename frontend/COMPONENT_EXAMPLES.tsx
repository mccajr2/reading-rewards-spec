import { useState } from 'react';
import {
  Alert,
  AlertDescription,
  AlertTitle,
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Modal,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from './src/components/ui';
import { FormField, PageGuidance } from './src/components/shared';

/**
 * ComponentExamples renders a lightweight visual gallery for major design-system
 * components. This file is documentation-oriented and can be mounted in a local
 * route or Storybook-style sandbox when needed.
 */
export function ComponentExamples() {
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <main style={{ maxWidth: 960, margin: '0 auto', padding: 24, display: 'grid', gap: 24 }}>
      <PageGuidance
        title="Component Examples"
        description="Reference examples for core design-system components."
        instructions="Use these patterns when creating new pages and flows."
        tone="parent"
      />

      <Card>
        <CardHeader>
          <CardTitle>Buttons & Badges</CardTitle>
          <CardDescription>Primary actions and status chips.</CardDescription>
        </CardHeader>
        <CardContent style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <Button>Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="ghost">Ghost</Button>
          <Badge>Default</Badge>
          <Badge variant="success">Success</Badge>
          <Badge variant="warning">Warning</Badge>
          <Badge variant="error">Error</Badge>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Form Field</CardTitle>
          <CardDescription>Label + input + helper/error pattern.</CardDescription>
        </CardHeader>
        <CardContent style={{ display: 'grid', gap: 12 }}>
          <FormField label="Email" type="email" placeholder="you@example.com" helperText="Use your parent account email" />
          <FormField label="Display Name" placeholder="Jamie" error="Display name is required" />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Alert States</CardTitle>
        </CardHeader>
        <CardContent style={{ display: 'grid', gap: 8 }}>
          <Alert variant="info">
            <AlertTitle>Info</AlertTitle>
            <AlertDescription>This is an informational alert.</AlertDescription>
          </Alert>
          <Alert variant="success">
            <AlertTitle>Success</AlertTitle>
            <AlertDescription>Changes were saved successfully.</AlertDescription>
          </Alert>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Tabs</CardTitle>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="overview">
            <TabsList>
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="details">Details</TabsTrigger>
            </TabsList>
            <TabsContent value="overview">Overview content</TabsContent>
            <TabsContent value="details">Details content</TabsContent>
          </Tabs>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Modal</CardTitle>
        </CardHeader>
        <CardContent>
          <Button onClick={() => setModalOpen(true)}>Open Modal</Button>
          <Modal open={modalOpen} onOpenChange={setModalOpen} title="Confirm Action" description="Review before confirming.">
            <p>Example modal body content.</p>
          </Modal>
        </CardContent>
      </Card>
    </main>
  );
}

export default ComponentExamples;
