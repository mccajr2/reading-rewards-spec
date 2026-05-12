# Quickstart Guide: Reward Customization Feature

**Version**: 1.0.0  
**Audience**: Parents and children learning to use reward system  
**Purpose**: Step-by-step walkthrough of common workflows

---

## Table of Contents

1. [Parent: Set Up Family Rewards](#parent-set-up-family-rewards)
2. [Parent: Create Per-Child Override](#parent-create-per-child-override)
3. [Child: Select Reward for Book](#child-select-reward-for-book)
4. [Child: Track Progress & Earn Reward](#child-track-progress--earn-reward)
5. [Child: Send Payout Reminder](#child-send-payout-reminder)
6. [Parent: Confirm Payout](#parent-confirm-payout)
7. [Parent: Send Encouragement](#parent-send-encouragement)

---

## Parent: Set Up Family Rewards

### Goal
Create family-wide reward options so all children can work toward them.

### Prerequisites
- You are logged in as a parent
- You have at least one child account created

### Steps

1. **Navigate to Manage Rewards**
   - Click "Manage Rewards" in main navigation (or go to `/rewards`)
   - You should see two sections: "Family Rewards" and "Per-Child Rewards"

2. **View Canned Templates**
   - Click "Suggested Rewards" or see pre-populated options:
     - $0.50 per chapter
     - $5.00 per book
     - 5 min screentime per chapter
     - 2 hours screentime per book
   - Each option shows the description and how child can earn

3. **Select & Add a Canned Reward**
   - Click "Add" on any canned option (e.g., "$0.50 per chapter")
   - System confirms: "Family reward added: $0.50 per chapter available to all children"
   - Reward appears in "Family Rewards" section

4. **Create a Custom Reward (Optional)**
   - Click "Create Custom Reward"
   - Fill in:
     - **Reward Type**: Money / Screentime / Custom
     - **Amount**: e.g., $1.00 or 30 min
     - **Unit**: Per Chapter / Per Book
     - **Description**: e.g., "Custom: $1 per chapter for readers"
   - Click "Save"
   - Reward now available to all children

5. **Verify**
   - You see both canned and custom rewards in "Family Rewards"
   - Next time any child logs in, they'll see these options

### Expected Outcome
- ✅ Family has 2+ reward options
- ✅ All children see these rewards in their "Select Reward" screen
- ✅ System defaults new children to $1/chapter if no explicit config

---

## Parent: Create Per-Child Override

### Goal
Set different rewards for individual children (e.g., Alice gets $1/chapter, Bob gets 30 min screentime/book).

### Prerequisites
- You have created family-level rewards (or want to override them)
- You have multiple children

### Steps

1. **Navigate to Per-Child Rewards**
   - Go to Manage Rewards → "Per-Child Rewards" section
   - You should see your children listed (e.g., "Alice", "Bob")

2. **Create Override for a Child**
   - Click "Create Reward for [Child Name]" (e.g., "Create Reward for Alice")
   - Fill in:
     - **Reward Type**: Money / Screentime / Custom
     - **Amount**: e.g., $1.00
     - **Unit**: Per Chapter / Per Book
     - **Description**: e.g., "$1 per chapter (Alice only)"
   - Click "Save"

3. **Verify**
   - Reward now appears under "Alice" in Per-Child Rewards
   - Alice will see this reward in her list (in addition to family rewards)
   - Other children don't see this reward

4. **Create Override for Another Child**
   - Repeat steps 1-3 for Bob with different settings (e.g., 30 min screentime per book)
   - Bob sees his override; Alice doesn't see Bob's

### Example Outcomes

**Family Rewards** (all children see):
- $0.50 per chapter
- 5 min screentime per chapter

**Alice's Per-Child Rewards**:
- $1.00 per chapter (override for Alice only)

**Bob's Per-Child Rewards**:
- 30 min screentime per book (override for Bob only)

---

## Child: Select Reward for Book

### Goal
Choose which family or personal reward to work toward when adding a book.

### Prerequisites
- You are logged in as a child
- Your parent has created family or per-child rewards
- You are adding a new book to read

### Steps

1. **Add a Book**
   - Click "Add Book" or search for a book
   - Find your book and click "Add This Book"

2. **System Prompts for Reward Selection**
   - You see: "What reward are you working toward?"
   - You see a list of available rewards:
     - $0.50 per chapter (family)
     - 5 min screentime per chapter (family)
     - $1.00 per chapter (just for you) ← if parent created per-child override

3. **Select Your Reward**
   - Click on the reward you want (e.g., "$1.00 per chapter")
   - Reward is now locked in for this book
   - You see confirmation: "Selected: $1.00 per chapter"

4. **Progress to Next Step**
   - System asks: "How many chapters does this book have?" (if per-chapter reward)
   - Or: "Track chapters or pages?" (if per-book reward)

5. **Verify**
   - When you open this book later, it shows your selected reward
   - Your rewards page shows this book being worked toward

### Expected Outcome
- ✅ Book has reward selected and locked in
- ✅ Reward doesn't change even if parent modifies the template
- ✅ You can change to a different reward for your **next** book

---

## Child: Track Progress & Earn Reward

### Goal
Mark chapters as read (or pages), complete the book, and earn your reward.

### Prerequisites
- You have selected a reward for a book
- You have started reading

### Steps

1. **Open Book Detail**
   - Click on the book in your Reading List
   - You see the book details and reward info: "$1.00 per chapter"

2. **Track by Chapters (If Per-Chapter Reward)**
   - Click "Update Progress"
   - You see chapter checkboxes:
     - [ ] Chapter 1: The Beginning
     - [ ] Chapter 2: The Quest
     - [ ] Chapter 3: The Challenge
     - ... etc
   - Check off chapters as you read them
   - System shows: "3 chapters read = $3.00 earned (so far)"

3. **Track by Pages (Optional)**
   - If you prefer tracking by pages instead:
   - Click "Track by Pages"
   - Enter total page count (system may suggest from OpenLibrary)
   - Slide to current page as you read
   - System shows progress percentage

4. **Complete the Book**
   - Once you've read all chapters, click "Mark as Complete"
   - System verifies you entered chapter count
   - System calculates: "10 chapters @ $1.00 = $10.00 earned"
   - You see confirmation: "Reward earned: $10.00"

5. **View New Balance**
   - Click on "Rewards" tab
   - You see: "Total Earned: $10.00, Available: $10.00"
   - Your new earning is listed in "Recent Activity"

### Expected Outcome
- ✅ Book marked as complete
- ✅ Reward calculated correctly (chapter count × rate)
- ✅ Earnings appear in your Rewards balance
- ✅ Status shows as "EARNED" (waiting for parent payout)

---

## Child: Send Payout Reminder

### Goal
Notify your parent that you have earned rewards and would like to be paid.

### Prerequisites
- You have earned at least $1 in rewards
- Your parent hasn't paid you yet (you have "Available Balance" > $0)

### Steps

1. **Open Your Rewards Page**
   - Click "Rewards" in main navigation
   - You see your balance:
     - Total Earned: $15.50
     - Available: $15.50 (waiting for parent payout)

2. **Send Payout Reminder**
   - Click "Send Payout Reminder" or "Notify Parent"
   - A text box appears with pre-filled message: "I've earned $15.50 and would like my payout!"
   - You can edit the message if desired
   - Click "Send"

3. **Confirmation**
   - System shows: "Message sent to Mom" or "Message sent to Dad"
   - You see the message in your "Messages" folder
   - Status shows: "Reminder sent on [date/time]"

4. **Wait for Parent Response**
   - Parent receives in-app notification + (optionally) email
   - Parent can confirm the payout in their Manage Rewards page
   - When parent confirms, you'll see in-app notification: "Your payout has been confirmed!"

### Expected Outcome
- ✅ Parent receives notification
- ✅ You can see sent message in history
- ✅ Status changes from "EARNED" to "PENDING_PAYOUT"
- ✅ Once parent confirms, you receive acknowledgment message

---

## Parent: Confirm Payout

### Goal
Mark your child's earned rewards as paid out to maintain an accurate ledger.

### Prerequisites
- Child has sent payout reminder
- You have received in-app notification (and/or email)
- You have paid the child in cash, check, etc.

### Steps

1. **Navigate to Manage Rewards**
   - Click "Manage Rewards"
   - Look for "Pending Payouts" section at top
   - You see: "Alice: $15.50 pending, Bob: $8.00 pending"

2. **View Child's Pending Earnings**
   - Click on "Alice" (or the pending child)
   - You see:
     - Total Earned: $45.50
     - Total Paid: $30.00
     - Available Balance: $15.50 (pending)
     - Recent reminders and confirmation history

3. **Confirm the Payout**
   - Click "Confirm Payout" or "Mark as Paid"
   - System shows breakdown:
     - $5.00 - Book 1 (10 chapters @ $0.50)
     - $10.50 - Book 2 (21 chapters @ $0.50)
   - Click "Confirm Payout"

4. **Verification**
   - System updates:
     - Total Paid: $45.50 (was $30.00)
     - Available Balance: $0.00
   - Status shows: "Payout confirmed on [date]"

5. **Child Receives Confirmation**
   - Alice receives in-app message: "Your payout of $15.50 has been confirmed!"
   - Alice sees her balance updated to reflect payout

6. **Audit Trail**
   - You can view "Payout History" to see all past payouts with dates and amounts
   - Supports accountability & dispute resolution

### Expected Outcome
- ✅ Payout marked as PAID in system
- ✅ Child receives in-app confirmation
- ✅ Balance updated for both parent and child
- ✅ Full audit trail maintained for records

---

## Parent: Send Encouragement

### Goal
Send a motivational message to your child to encourage reading.

### Prerequisites
- You are logged in as parent
- You want to send a quick encouragement message

### Steps

1. **Navigate to Child Profile or Rewards**
   - Either: Click on child's name in your dashboard
   - Or: Go to Manage Rewards and click on child's name
   - Or: Any child profile page

2. **Send Message**
   - Look for "Send Message" button or pencil icon
   - Click "Send Encouragement"
   - A text box appears
   - Type your message:
     - "Great job on finishing that book! 📚"
     - "You're a reading superstar! Keep it up!"
     - "I'm so proud of your progress!"
   - Click "Send"

3. **Confirmation**
   - System shows: "Message sent to Alice!"
   - You see the message in "Sent Messages" (for your own records)

4. **Child Receives Message**
   - Alice receives in-app notification: "New message from Mom"
   - She clicks notification to view: "Great job on finishing that book! 📚"
   - Message appears in her "Messages" folder
   - NO EMAIL sent to Alice (protects privacy)

5. **View Message History**
   - Go to child's profile → "Messages"
   - See all encouragement messages you've sent (Alice can also see them)
   - Messages include date/time stamps

### Expected Outcome
- ✅ Child receives in-app notification only (no email)
- ✅ Message persisted in history
- ✅ Child can review encouragement anytime
- ✅ Strengthens parent-child communication

---

## FAQ & Troubleshooting

### Q: What if my child doesn't see their per-child reward?
- **A**: Make sure you saved it. Go to Manage Rewards → Per-Child Rewards, and verify it appears under their name. If not visible to child, try logging child out and back in to refresh rewards list.

### Q: Can I change a reward after my child started reading?
- **A**: Yes! Parent can edit reward amount. However, child's current book will use the **locked-in rate** from when they selected it. The new rate applies to **future books**. This is fair to the child.

### Q: What if I delete a reward?
- **A**: System archives it (soft-delete). Child can finish their current book at the old rate. The reward won't be available for new books. No data is lost.

### Q: Can my child change rewards mid-book?
- **A**: Yes! They can switch to a different reward. The new reward takes effect for chapters starting from the next one (not retroactive). Old chapters use old rate; new chapters use new rate.

### Q: How often do I get payout reminder emails?
- **A**: By default, 1 email per day per child (even if child sends multiple reminders). You can adjust email settings in your account preferences (opt-out completely if desired).

### Q: What if my child has mixed reward types (money + screentime)?
- **A**: Rewards page shows each type separately. E.g., "Earned: $15.50 cash" and "Earned: 120 minutes screentime". Both tracked independently.

---

## Architecture Notes for Developers

### Key Entities
- **RewardTemplate**: Parent config (family or per-child)
- **RewardSelection**: Child's choice for a book (locked in)
- **ProgressTracking**: Chapter/page progress
- **RewardAccumulation**: Immutable ledger (EARNED → PENDING → PAID)
- **Message**: In-app communication

### Important Constraints
- Reward rate is **locked in** at selection time → supports fairness & avoids "moving goalposts"
- RewardAccumulation is **append-only** → supports audit trail (SC-003)
- Soft-delete RewardTemplate → allows archive & recovery
- Optional email notifications → respects family privacy

### Performance Considerations
- Rewards page must load <1s (SC-005) → use computed balance view
- Paginate message lists and history (limit 50 default, max 100)
- Cache available rewards list (recompute only on template change)

---

## Success Metrics

| Metric | Target | Notes |
|--------|--------|-------|
| Parent adoption | 80% configure reward within week | Feature is underused if <50% |
| Child engagement | +15% daily reading frequency | Measure over 4-week period post-launch |
| Payout accuracy | 100% (zero discrepancies) | Spot-check 50 random payouts |
| Payout reminder response | 95% within 48 hours | Parent responsiveness |
| Rewards page load | <1s on 4G throttle | Lighthouse metric (SC-005) |
| Accessibility | WCAG 2.1 AA (zero critical) | Lighthouse audit (SC-006) |
| Default adoption | 100% new children at $1/chapter | Zero-config start (SC-007) |

---

## Phase 13 Validation Runbook

Run these checks before release signoff:

1. Regression E2E flow
   - `npm run test:e2e -- tests/e2e/reward-customization.spec.ts`
2. Accessibility E2E flow
   - `npm run test:e2e -- tests/e2e/reward-accessibility.spec.ts`
3. Frontend performance baseline
   - `node scripts/phase8-rewards-performance-audit.mjs`

Expected results:

- Reward customization E2E passes parent->child->payout loop.
- Accessibility scan reports zero `critical` and `serious` WCAG violations on child and parent rewards pages.
- Lighthouse report generated at `test-results/perf/rewards-lighthouse.json` and summary at `test-results/perf/rewards-lighthouse-summary.json`.

### Screenshot Checklist (Release Evidence)

- Parent Manage Rewards page showing family rewards and encouragement composer.
- Child rewards page showing reward type cards (cash/time/custom).
- Child payout reminder confirmation state.
- Parent payout reminders inbox and read-state transition.
- Child message center displaying encouragement history.

---

**Next Steps**: Phase 2 will break these workflows into implementation tasks (backend services, API controllers, frontend components, tests).
