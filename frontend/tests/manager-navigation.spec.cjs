const { test, expect } = require("@playwright/test");
const { login, command, account, SITE } = require("./support/helpers.cjs");
const { randomUUID } = require("node:crypto");

test("manager menu contains eight sections and directory shows unit relationships", async ({
  page,
}) => {
  await login(page);
  await expect(
    page.locator("nav.manager-navigation a > span:first-of-type"),
  ).toHaveText([
    "Overview",
    "Directory",
    "Financial management",
    "Announcements",
    "Service requests",
    "Polls",
    "My profile",
    "Notifications",
  ]);
  await page.getByRole("link", { name: "Directory", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Directory", exact: true }).first(),
  ).toBeVisible();
  await expect(page.locator(".unit-directory")).toContainText("A-12");
  await page.getByRole("link", { name: "A-12", exact: true }).click();
  await expect(page.locator(".relationship-list")).toContainText(
    "Deniz Resident",
  );
  await expect(page.locator(".relationship-list")).toContainText("Tenant");
  await expect(page.locator(".unit-detail")).not.toContainText("Total charged");
  await page
    .getByRole("button", { name: "Close menu", exact: true })
    .first()
    .click();
  await expect(page.locator("aside.sidebar")).not.toBeVisible();
  await page.getByRole("button", { name: "Open menu", exact: true }).click();
  await expect(page.locator("aside.sidebar")).toBeVisible();
});

test("dues totals, remaining amounts and unit/year/month filters use persisted charge types", async ({
  page,
}) => {
  await login(page);
  const now = new Date();
  const month = now.toISOString().slice(0, 7);
  const year = month.slice(0, 4);
  const amount = 13579;
  for (const [category, periodStart, description] of [
    ["DUES", month + "-01", "Current dues fixture"],
    ["DUES", Number(year) - 1 + "-02-01", "Previous dues fixture"],
    ["OTHER", month + "-01", "Roof repair fixture"],
  ]) {
    const summary = await account(page, "a03");
    const d = new Date(periodStart + "T12:00:00Z");
    const end = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0))
      .toISOString()
      .slice(0, 10);
    await command(
      page,
      "/charge-batches",
      {
        items: [
          {
            unitId: "a03",
            category,
            description,
            periodStart,
            periodEnd: end,
            dueOn: periodStart,
            amountMinor: amount,
            currency: "TRY",
            expectedAccountVersion: summary.version,
          },
        ],
      },
      randomUUID(),
    );
  }
  await page.goto("/finance");
  const records = (
    await (await page.request.get(SITE + "/charges?limit=100")).json()
  ).items;
  const dues = records.filter(
    (item) => item.category === "DUES" && !item.cancelledAt,
  );
  const format = (value) =>
    new Intl.NumberFormat("en-GB", {
      style: "currency",
      currency: "TRY",
      maximumFractionDigits: 2,
    }).format(value / 100);
  await expect(
    page.locator(".tracking-widgets article").first().locator("strong"),
  ).toHaveText(
    format(dues.reduce((sum, item) => sum + item.outstandingMinor, 0)),
  );
  await expect(
    page.locator(".tracking-widgets article").nth(1).locator("strong"),
  ).toHaveText(
    format(
      dues
        .filter((item) => item.periodStart.slice(0, 7) === month)
        .reduce((sum, item) => sum + item.outstandingMinor, 0),
    ),
  );
  await expect(page.locator(".charge-tracking-table")).not.toContainText(
    "Roof repair fixture",
  );
  await page
    .getByLabel("Search unit or description", { exact: true })
    .fill("A-03");
  await page.getByLabel("Year", { exact: true }).selectOption(year);
  await page
    .getByLabel("Month", { exact: true })
    .selectOption(month.slice(5, 7));
  await expect(page.locator(".charge-tracking-table tbody tr")).toHaveCount(
    dues.filter(
      (item) => item.unitId === "a03" && item.periodStart.slice(0, 7) === month,
    ).length,
  );
  await expect(page.locator(".charge-tracking-table")).not.toContainText(
    "Previous dues fixture",
  );
  await expect(page.locator(".charge-tracking-table")).toContainText(
    "Current dues fixture",
  );
  await page.getByRole("link", { name: "Other charges", exact: true }).click();
  await expect(page.locator(".charge-tracking-table")).toContainText(
    "Roof repair fixture",
  );
  await expect(page.locator(".charge-tracking-table")).not.toContainText(
    "Current dues fixture",
  );
});

test("other charge form persists OTHER and balance shows real account balances", async ({
  page,
}) => {
  await login(page);
  await page.goto("/finance/other");
  await page
    .getByRole("button", { name: "Create other charge", exact: true })
    .click();
  const description = "One-off repair " + Date.now();
  await page.getByLabel("DESCRIPTION", { exact: true }).fill(description);
  await page.getByLabel("Amount (TRY)", { exact: true }).fill("45");
  await page.getByRole("checkbox", { name: "A-12", exact: true }).check();
  const response = page.waitForResponse(
    (r) =>
      r.url().endsWith("/charge-batches") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  const saved = await response;
  expect(saved.status()).toBe(201);
  expect(saved.request().postDataJSON().items[0].category).toBe("OTHER");
  await page.reload();
  await expect(page.locator(".charge-tracking-table")).toContainText(
    description,
  );
  await page.getByRole("link", { name: "Balance", exact: true }).click();
  await expect(page.locator(".balance-cards")).toContainText("Community Bank");
  const accounts = (
    await (
      await page.request.get(SITE + "/financial-accounts?limit=100")
    ).json()
  ).items;
  for (const account of accounts) {
    const card = page.locator(".balance-cards article").filter({
      has: page.getByRole("heading", { name: account.name, exact: true }),
    });
    await expect(card.locator("strong")).toHaveText(
      new Intl.NumberFormat("en-GB", {
        style: "currency",
        currency: "TRY",
      }).format(account.balanceMinor / 100),
    );
  }
});

test("mobile drawer opens and closes, navigates and keeps finance within the viewport", async ({
  page,
}, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page);
  await expect(page.locator("aside.sidebar")).not.toBeVisible();
  await page.getByRole("button", { name: "Open menu", exact: true }).click();
  await expect(page.locator("aside.sidebar")).toBeVisible();
  await page
    .getByRole("link", { name: "Financial management", exact: true })
    .click();
  await expect(page.locator("aside.sidebar")).not.toBeVisible();
  await expect(
    page.getByRole("link", { name: "Dues tracking", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth - window.innerWidth,
    ),
  ).toBeLessThanOrEqual(1);
  await page.screenshot({
    path: testInfo.outputPath("manager-finance-mobile.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Open menu", exact: true }).click();
  await page.keyboard.press("Escape");
  await expect(page.locator("aside.sidebar")).not.toBeVisible();
  await page
    .getByRole("combobox", { name: "Language", exact: true })
    .selectOption("tr");
  await expect(
    page.getByRole("link", { name: "Aidat takibi", exact: true }),
  ).toBeVisible();
});
