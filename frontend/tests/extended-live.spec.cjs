const { test, expect } = require("@playwright/test");
const { login, command, SITE } = require("./support/helpers.cjs");

test("expenses settle and reverse through the financial service", async ({
  page,
}) => {
  await login(page);
  await page.locator('nav a[href="/finance"]').click();
  await page
    .getByRole("button", { name: "Record expense", exact: true })
    .click();
  const description = "UI repair " + Date.now();
  await page.getByLabel("DESCRIPTION", { exact: true }).fill(description);
  await page.getByLabel("Amount (TRY)", { exact: true }).fill("50");
  await page
    .getByLabel("Vendor / supplier", { exact: true })
    .fill("Browser Supplier");
  await page
    .getByRole("checkbox", {
      name: "Record payment for this expense now",
      exact: true,
    })
    .check();
  const saved = page.waitForResponse(
    (r) => r.url().endsWith("/expenses") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  expect((await saved).status()).toBe(201);
  const row = page.getByRole("row").filter({ hasText: description });
  await expect(row).toContainText("Paid");
  await row
    .getByRole("button", { name: "Reverse expense payment", exact: true })
    .click();
  await page
    .getByLabel("Correction reason", { exact: true })
    .fill("Wrong expense account");
  await page
    .getByRole("button", { name: "Confirm correction", exact: true })
    .click();
  await expect(row).toContainText("Outstanding");
  await page.reload();
  await page.getByRole("button", { name: "Expenses", exact: true }).click();
  await expect(page.locator(".expense-audit")).toContainText(
    "Wrong expense account",
  );
});

test("manager notification review does not create a financial payment", async ({
  page,
  request,
}) => {
  const csrf = await (await request.get("/api/v1/auth/csrf")).json();
  const auth = await (
    await request.post("/api/v1/auth/login", {
      data: { email: "resident@govener.test", password: "DemoPass123!" },
      headers: { "X-CSRF-Token": csrf.csrfToken },
    })
  ).json();
  const reference = "Review UI " + Date.now();
  const response = await request.post(SITE + "/payment-notifications", {
    data: {
      unitId: "a12",
      amountMinor: 1234,
      currency: "TRY",
      paymentDate: new Date().toISOString().slice(0, 10),
      reference,
      note: "",
    },
    headers: { "X-CSRF-Token": auth.csrfToken },
  });
  expect(response.ok()).toBe(true);
  await login(page);
  const before = await (
    await page.request.get(SITE + "/unit-accounts/a12")
  ).json();
  await page.locator('nav a[href="/payments"]').click();
  const row = page.locator(".payment-row").filter({ hasText: reference });
  await row
    .getByRole("button", { name: "Review notification", exact: true })
    .click();
  await row
    .getByLabel("Review note / rejection reason", { exact: true })
    .fill("Checked bank statement");
  await row.getByRole("button", { name: "Mark reviewed", exact: true }).click();
  await expect(row).toContainText("Reviewed");
  const after = await (
    await page.request.get(SITE + "/unit-accounts/a12")
  ).json();
  expect(after.paidMinor).toBe(before.paidMinor);
  await page.reload();
  await expect(
    page.locator(".payment-row").filter({ hasText: reference }),
  ).toContainText("Reviewed");
});

test("notification read state is persisted and scoped to the current user", async ({
  page,
}) => {
  await login(page);
  const notice = await command(page, "/announcements", {
    title: "Notification " + Date.now(),
    body: "Community update",
    category: "COMMUNITY",
    audience: { type: "ALL", ids: [] },
    status: "PUBLISHED",
  });
  await expect
    .poll(async () =>
      (
        await (
          await page.request.get(SITE + "/notifications?unread=true")
        ).json()
      ).items.some((item) => item.target.id === notice.id),
    )
    .toBe(true);
  await page.goto("/notifications");
  const marked = page.waitForResponse(
    (response) =>
      response.url().endsWith("/notifications/read-all") &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Mark all as read", exact: true })
    .click();
  expect((await marked).status()).toBe(204);
  await expect(page.locator(".notification-list article.unread")).toHaveCount(
    0,
  );
  await expect(
    page.getByRole("button", { name: "Mark all as read", exact: true }),
  ).toBeDisabled();
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Mark all as read", exact: true }),
  ).toBeDisabled();
  expect(
    (await (await page.request.get(SITE + "/notifications?unread=true")).json())
      .items,
  ).toHaveLength(0);
});

test("request collections follow server cursors beyond one hundred records", async ({
  page,
}) => {
  await login(page);
  const prefix = "Paged " + Date.now();
  for (let i = 0; i < 103; i++)
    await command(page, "/requests", {
      unitId: "a12",
      title: prefix + " " + i,
      location: "Lobby",
      description: "Pagination fixture",
      photoIds: [],
    });
  const cursors = [];
  page.on("request", (r) => {
    const url = new URL(r.url());
    if (url.pathname.endsWith("/requests") && r.method() === "GET")
      cursors.push(url.searchParams.get("cursor"));
  });
  await page.goto("/requests");
  await page
    .getByRole("textbox", { name: "Search requests", exact: true })
    .fill(prefix);
  await expect(page.locator("tbody tr")).toHaveCount(103);
  expect(cursors.some(Boolean)).toBe(true);
});

test("a late response from the previous community cannot overwrite a new selection", async ({
  page,
}) => {
  await login(page, "resident");
  let release;
  const gate = new Promise((resolve) => (release = resolve));
  let intercepted;
  const seen = new Promise((resolve) => (intercepted = resolve));
  await page.route(
    "**/api/v1/communities/olive-gardens/directory?*",
    async (route) => {
      intercepted();
      await gate;
      await route.continue().catch(() => {});
    },
  );
  await page
    .getByRole("button", { name: "Refresh information", exact: true })
    .click();
  await seen;
  await page
    .getByRole("combobox", { name: "Community", exact: true })
    .selectOption("bosphorus-homes");
  release();
  await expect(page.locator(".workspace strong")).toHaveText("Bosphorus Homes");
  await expect(
    page.getByRole("combobox", { name: "My unit", exact: true }),
  ).toHaveValue("b06");
  await page.goto("/units");
  await expect(page.locator(".resident-directory")).not.toContainText(
    "Household Member",
  );
});

test("the live workspace remains usable on a phone, including sign out", async ({
  page,
}, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, "resident");
  await page.goto("/units");
  await expect(page.locator(".resident-directory")).toBeVisible();
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth -
        document.documentElement.clientWidth,
    ),
  ).toBeLessThanOrEqual(1);
  await page.screenshot({
    path: testInfo.outputPath("mobile-live.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
});
