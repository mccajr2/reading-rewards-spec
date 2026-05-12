import { expect, test } from '@playwright/test';
import { apiUrl, createKid, login, signupAndVerify, uniqueEmail, uniqueUsername } from './helpers';

test.describe('Reward customization regression flow', () => {
  test('parent config -> child select -> earn -> remind -> payout', async ({ request, page }) => {
    const parentEmail = uniqueEmail('phase13-parent');
    const parentPassword = 'Password1!';
    const childUsername = uniqueUsername('phase13-kid');
    const childPassword = 'KidPass1!';

    await signupAndVerify(request, parentEmail, parentPassword, 'Phase13', 'Parent');
    const parentToken = await login(request, parentEmail, parentPassword);
    await createKid(request, parentToken, childUsername, 'PhaseKid', childPassword);
    const childToken = await login(request, childUsername, childPassword);

    const parentHeaders = {
      Authorization: `Bearer ${parentToken}`,
      'Content-Type': 'application/json',
    };
    const childHeaders = {
      Authorization: `Bearer ${childToken}`,
      'Content-Type': 'application/json',
    };

    const createRewardRes = await request.post(apiUrl('/parent/rewards'), {
      headers: parentHeaders,
      data: {
        childId: null,
        rewardType: 'MONEY',
        amount: 1,
        unit: 'PER_CHAPTER',
        frequency: 'IMMEDIATE',
        cannedTemplateId: null,
        description: '$1 per chapter regression reward',
      },
    });
    expect(createRewardRes.ok()).toBeTruthy();

    const bookPayload = {
      googleBookId: `e2e-reward-customization-${Date.now()}`,
      title: 'E2E Reward Journey Book',
      authors: ['Spec Kit'],
      description: 'Regression flow test book',
      thumbnailUrl: null,
    };
    const addBookRes = await request.post(apiUrl('/books'), {
      headers: childHeaders,
      data: bookPayload,
    });
    expect(addBookRes.ok()).toBeTruthy();
    const bookRead = await addBookRes.json();

    const availableRewardsRes = await request.get(apiUrl('/child/rewards/available'), {
      headers: { Authorization: `Bearer ${childToken}` },
    });
    expect(availableRewardsRes.ok()).toBeTruthy();
    const availableRewardsBody = await availableRewardsRes.json();
    const firstRewardId = availableRewardsBody.availableRewards[0].rewardTemplateId;

    const selectRewardRes = await request.post(apiUrl(`/child/rewards/select/${bookRead.id}`), {
      headers: childHeaders,
      data: { rewardTemplateId: firstRewardId },
    });
    expect(selectRewardRes.ok()).toBeTruthy();

    const saveChaptersRes = await request.post(apiUrl(`/bookreads/${bookRead.id}/chapters`), {
      headers: childHeaders,
      data: [{ name: 'Chapter 1', chapterIndex: 0 }],
    });
    expect(saveChaptersRes.ok()).toBeTruthy();
    const chapters = await saveChaptersRes.json();

    const markChapterReadRes = await request.post(apiUrl(`/bookreads/${bookRead.id}/chapters/${chapters[0].id}/read`), {
      headers: { Authorization: `Bearer ${childToken}` },
    });
    expect(markChapterReadRes.ok()).toBeTruthy();

    const finishRes = await request.post(apiUrl(`/books/${bookRead.googleBookId}/finish`), {
      headers: { Authorization: `Bearer ${childToken}` },
    });
    expect(finishRes.ok()).toBeTruthy();

    const balanceBeforeReminderRes = await request.get(apiUrl('/child/rewards/balance'), {
      headers: { Authorization: `Bearer ${childToken}` },
    });
    expect(balanceBeforeReminderRes.ok()).toBeTruthy();
    const balanceBeforeReminder = await balanceBeforeReminderRes.json();
    const pendingAmount = Number(balanceBeforeReminder.balance.availableBalance ?? 0);
    expect(pendingAmount).toBeGreaterThan(0);

    const reminderRes = await request.post(apiUrl('/child/rewards/messages/payout-reminder'), {
      headers: childHeaders,
      data: {
        pendingAmount,
        note: 'Regression reminder',
        emailEnabled: false,
      },
    });
    expect(reminderRes.status()).toBe(201);

    const remindersRes = await request.get(apiUrl('/parent/rewards/messages/payout-reminders'), {
      headers: { Authorization: `Bearer ${parentToken}` },
    });
    expect(remindersRes.ok()).toBeTruthy();
    const reminders = await remindersRes.json();
    expect(Number(reminders.unreadCount)).toBeGreaterThan(0);

    const childListRes = await request.get(apiUrl('/parent/kids'), {
      headers: { Authorization: `Bearer ${parentToken}` },
    });
    expect(childListRes.ok()).toBeTruthy();
    const childList = await childListRes.json();
    const childId = childList.find((row: { username: string }) => row.username === childUsername)?.id;
    expect(childId).toBeTruthy();

    const accumulationsRes = await request.get(apiUrl(`/parent/rewards/child/${childId}/accumulation`), {
      headers: { Authorization: `Bearer ${parentToken}` },
    });
    expect(accumulationsRes.ok()).toBeTruthy();
    const accumulationsBody = await accumulationsRes.json();
    const accumulationIds = (accumulationsBody.accumulations ?? []).map((row: { id: string }) => row.id);
    expect(accumulationIds.length).toBeGreaterThan(0);

    const payoutRes = await request.post(apiUrl(`/parent/rewards/child/${childId}/payout-confirm`), {
      headers: parentHeaders,
      data: {
        accumulationIds,
        payoutMethod: 'cash',
      },
    });
    expect(payoutRes.ok()).toBeTruthy();

    const balanceAfterPayoutRes = await request.get(apiUrl('/child/rewards/balance'), {
      headers: { Authorization: `Bearer ${childToken}` },
    });
    expect(balanceAfterPayoutRes.ok()).toBeTruthy();
    const balanceAfterPayout = await balanceAfterPayoutRes.json();
    expect(Number(balanceAfterPayout.balance.availableBalance ?? 0)).toBe(0);

    await page.goto('/login');
    await page.getByLabel('Username or Email').fill(childUsername);
    await page.getByLabel('Password').fill(childPassword);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).not.toHaveURL(/\/login/, { timeout: 8_000 });
    await page.goto('/child/rewards');
    await expect(page.getByRole('heading', { name: /your rewards shop/i })).toBeVisible();
  });
});