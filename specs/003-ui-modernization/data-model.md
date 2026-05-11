# Data Model: UI Components & Page Guidance

**Phase**: 1 | **Feature**: `003-ui-modernization` | **Date**: 2026-05-11

## Component Taxonomy (Atomic Design)

### Atoms (Primitive, Reusable)

- **Button**: Primary, secondary, outline, ghost variants; sizes: sm, md, lg; support loading, disabled states
- **Input**: Text, email, password, number; with label, placeholder, error messaging
- **Checkbox**: With label, helper text, disabled state
- **Radio Group**: Multi-option selector with labels
- **Select/Dropdown**: Combobox with search, keyboard navigation
- **Icon**: Lucide React icons; support sizing and color customization
- **Badge**: Status badges (success, warning, error, neutral)
- **Label**: Form field labels with required indicator
- **Text**: Heading (h1-h6), Paragraph, Caption, Code (monospace)

### Molecules (Combinations of Atoms)

- **FormField**: Label + Input + Error Message + Helper Text (DRY form composition)
- **Card**: Bordered container with padding, shadow; optional header/footer
- **Alert**: Message container with icon, title, description; variants (info, success, warning, error)
- **Navigation Item**: Icon + Label + Active indicator (for sidebar/menu)
- **Search Bar**: Input + Submit Button + Clear Button
- **Pagination**: Previous/Next buttons + page numbers + current indicator
- **Modal Header**: Title + Close button
- **Tab Button**: Text + active state + underline indicator

### Organisms (Complex, Page-Level)

- **PageGuidance**: Title + Description + Instructions (wrapped at top of page)
- **Sidebar Navigation**: Logo + Nav Items + Logout Button
- **Page Header**: Navigation + User Profile Menu + Settings link
- **Form**: Multiple FormField molecules + Submit/Cancel buttons
- **Data Table**: Sortable columns, row selection, pagination
- **Modal**: Header + Content + Footer (with buttons)
- **Book Card**: Thumbnail + Title + Author + Rating + Add/Remove buttons

---

## Page Layout Model

All pages follow this structure:

```
┌─────────────────────────────────────────┐
│ Page Header (Navigation + User Menu)    │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ PageGuidance:                       │ │
│ │ • Title (page name)                 │ │
│ │ • Description (what this page does) │ │
│ │ • Instructions (how to use it)      │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Page Content                        │ │
│ │ (dashboard, forms, lists, etc.)     │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ Footer (optional)                       │
└─────────────────────────────────────────┘
```

**Mobile (<640px)**: Single column, sidebar collapses to hamburger menu  
**Tablet (768px+)**: Two columns, sidebar visible  
**Desktop (1024px+)**: Full sidebar, expanded content area

---

## Page Descriptions & Guidance

### Parent Pages

#### 1. Login Page
**Purpose**: Authenticate parent account  
**Guidance Title**: "Welcome Back"  
**Guidance Description**: "Log in to your Reading Rewards account to manage your child's reading activity and rewards."  
**Instructions**: "Enter your email and password. If you don't have an account yet, click 'Sign Up' to create one."  
**Tone**: Professional, welcoming  
**Key Components**: Email Input, Password Input, Login Button, "Forgot Password?" link, Sign Up link

#### 2. Dashboard (Parent)
**Purpose**: Overview of all children's reading activity, rewards, and milestones  
**Guidance Title**: "Your Dashboard"  
**Guidance Description**: "Here's a snapshot of each child's reading progress. You can see books read, rewards earned, and upcoming milestones at a glance."  
**Instructions**: "Click on any child's card to view detailed progress, or use the menu to manage accounts and settings."  
**Tone**: Professional, informative  
**Key Components**: Child Summary Cards (books read, rewards, next milestone), Recent Activity feed, "Add Child" button, Menu (Settings, Child Management, Logout)

#### 3. Child Account Management
**Purpose**: Add, edit, delete child accounts; set reading goals and reward preferences  
**Guidance Title**: "Manage Child Accounts"  
**Guidance Description**: "Create or manage accounts for each of your children. Set personalized reading goals and choose rewards they can unlock."  
**Instructions**: "Click 'Add Child' to create a new account, or click on a child's name to edit their settings (goals, rewards). You can also remove a child's account from this page."  
**Tone**: Professional, clear action-oriented  
**Key Components**: Child List (table or cards), "Add Child" button, Edit/Delete buttons per child, Child settings form (name, age, reading goal, reward preferences)

#### 4. Rewards Settings
**Purpose**: Configure rewards catalog, points, redemption rules  
**Guidance Title**: "Rewards Settings"  
**Guidance Description**: "Customize the rewards your children can earn. You can create new rewards, set point values, and manage redemption."  
**Instructions**: "Click 'Add Reward' to create a new reward item. Edit or delete existing rewards using the action buttons. Changes take effect immediately."  
**Tone**: Professional, technical but not jargon-heavy  
**Key Components**: Rewards table/list, "Add Reward" button, Edit/Delete per reward, Reward form (title, description, points cost, redemption link)

### Child Pages

#### 1. Reading List
**Purpose**: Track books read, in-progress, and want-to-read  
**Guidance Title**: "Your Reading List 📚"  
**Guidance Description**: "This is your personal library! Keep track of books you've read, are reading right now, and want to read soon."  
**Instructions**: "Click 'Add Book' to search for a new book. Click on any book in your list to update your reading progress or leave a note. Books you finish earn you rewards!"  
**Tone**: Fun, encouraging, celebratory  
**Key Components**: Tabs (Read, Reading Now, Want to Read), Book cards (thumbnail, title, author, status, action buttons), "Add Book" button, Search bar

#### 2. Book Search
**Purpose**: Search for and add books to reading list  
**Guidance Title**: "Find Your Next Book 🔍"  
**Guidance Description**: "Search for any book and add it to your reading list. You can search by title, author, or keyword."  
**Instructions**: "Type in the search box to find a book. Click 'Add to List' to save it. Once you finish reading, come back to update your progress!"  
**Tone**: Fun, exploratory, helpful  
**Key Components**: Search input, Search results list (book cards with Add button), Empty state ("No books found—try a different search!")

#### 3. Reading Progress
**Purpose**: Log reading sessions, track pages/chapters, update status  
**Guidance Title**: "Log Your Reading 📖"  
**Guidance Description**: "Tell us how many pages you've read, and we'll track your progress toward finishing this book!"  
**Instructions**: "Enter the number of pages or chapters you read today. You can also add a note (e.g., 'Really exciting chapter!'). When you finish the book, mark it as complete!"  
**Tone**: Fun, celebratory (e.g., "Great job! 🎉 You're 50% done!"), encouraging  
**Key Components**: Current book title, Pages/chapters input, Optional note field, Submit button, Progress bar (visual %), Reward preview (e.g., "+50 points if you finish!")

#### 4. Rewards Shop
**Purpose**: View available rewards and redeem earned points  
**Guidance Title**: "Your Rewards Shop 🎁"  
**Guidance Description**: "Here are all the rewards you can unlock with your reading points. The more you read, the more rewards you can earn!"  
**Instructions**: "Click on any reward to see how many points you need. Once you have enough points, click 'Redeem' to claim your reward!"  
**Tone**: Fun, celebratory, motivating (e.g., "You're 100 points away from [reward]!")  
**Key Components**: Rewards grid/list (cards showing title, image, points cost, unlock button), Current points display (prominent), Locked/Unlocked state, Redeem confirmation modal

---

## Design Tokens

### Colors

**Semantic**:
- `--color-primary`: #2563eb (Blue-600) — Primary CTAs, parent focus
- `--color-primary-light`: #eff6ff (Blue-50) — Backgrounds, parent subtle accents
- `--color-accent`: #fbbf24 (Amber-400) — Child focus, celebratory
- `--color-accent-light`: #fffbeb (Amber-50) — Child page backgrounds
- `--color-success`: #16a34a (Green-600) — Positive outcomes
- `--color-warning`: #ea580c (Orange-600) — Attention needed
- `--color-error`: #dc2626 (Red-600) — Errors, destructive actions
- `--color-text-primary`: #111827 (Gray-900) — Body text
- `--color-text-secondary`: #4b5563 (Gray-700) — Secondary text
- `--color-border`: #e5e7eb (Gray-200) — Borders, dividers
- `--color-background`: #ffffff (White) — Page backgrounds
- `--color-background-alt`: #f9fafb (Gray-50) — Alternate backgrounds

### Typography

**Font Family**: system-ui, -apple-system, "Segoe UI", sans-serif

**Scales**:
- `font-size-xs`: 12px
- `font-size-sm`: 14px
- `font-size-base`: 16px
- `font-size-lg`: 18px
- `font-size-xl`: 20px
- `font-size-2xl`: 24px
- `font-size-3xl`: 28px

**Line Heights**:
- Parent: 1.5 (relaxed)
- Child: 1.6 (extra relaxed, more breathing room)

**Font Weights**:
- Regular: 400
- Medium: 500
- Semibold: 600
- Bold: 700

### Spacing

**Base Unit**: 4px  
**Scale**: 4, 8, 12, 16, 20, 24, 32, 40, 48, 56, 64px

**Common Margins**:
- Tight: 8px
- Normal: 16px
- Loose: 24px
- Extra loose: 32px

**Common Paddings**:
- Card padding: 24px
- Section padding: 32px
- Button padding: 12px 16px (vertical × horizontal)
- Form field spacing: 16px gap between fields

---

## Accessibility Attributes

All components must include:
- Semantic HTML (`<button>`, `<input>`, `<label>`, `<nav>`)
- ARIA labels: `aria-label`, `aria-describedby`, `aria-current`, `aria-expanded`
- Focus indicators: `focus:ring-2 focus:ring-blue-500`
- Minimum touch targets: 44px × 44px (`min-h-11 min-w-11`)
- Color contrast: ≥4.5:1 for AA compliance
- Keyboard navigation: Tab, Enter, Escape, Arrow keys

---

## Tone Guidelines Summary

| Audience | Tone | Example |
|----------|------|---------|
| **Parent** | Professional, clear, informative | "Your dashboard provides a snapshot of each child's reading activity. Use this view to monitor progress at a glance." |
| **Child** | Fun, encouraging, celebratory | "Here's your reading list! 📚 Want to add a new book? Click the 'Add Book' button and search for it. The more you read, the more rewards you earn!" |
