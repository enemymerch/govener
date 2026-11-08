const { test, expect } = require("@playwright/test");
const fs = require("node:fs");
const path = require("node:path");
const {
  login,
  command,
  account,
  charge,
  SITE,
} = require("./support/helpers.cjs");

test("translation catalogs match and the login supports Turkish and English", async ({
  page,
}) => {
  const en = JSON.parse(
    fs.readFileSync(path.join(__dirname, "../src/locales/en.json"), "utf8"),
  );
  const tr = JSON.parse(
    fs.readFileSync(path.join(__dirname, "../src/locales/tr.json"), "utf8"),
  );
  expect(Object.keys(en).sort()).toEqual(Object.keys(tr).sort());
  for (const key of Object.keys(en)) {
    expect(tr[key].trim()).not.toBe("");
    const vars = (s) =>
      [...s.matchAll(/{{\s*(\w+)\s*}}/g)].map((m) => m[1]).sort();
    expect(vars(en[key]), key).toEqual(vars(tr[key]));
  }
  await page.goto("/login");
  await expect(page.locator("html")).toHaveAttribute("lang", "tr");
  await expect(page.locator(".brand")).toHaveText("yönetici.");
  await page
    .getByRole("combobox", { name: "Dil", exact: true })
    .selectOption("en");
  await expect(
    page.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
});

test("server credentials, HttpOnly sessions, reload and logout replace demo roles", async ({
  page,
}) => {
  await page.addInitScript(() => {
    sessionStorage.setItem("govener.demoRole", "Manager");
    localStorage.setItem("govener.language", "en");
  });
  await page.goto("/overview");
  await expect(
    page.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
  await page
    .getByLabel("Email address", { exact: true })
    .fill("manager@govener.test");
  await page.getByLabel("Password", { exact: true }).fill("wrong-password");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Email or password is incorrect",
  );
  await page.getByLabel("Password", { exact: true }).fill("DemoPass123!");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.locator(".app-shell")).toBeVisible();
  expect(
    (await page.context().cookies()).find((c) => c.name === "govener_session")
      .httpOnly,
  ).toBe(true);
  await expect(page.getByLabel("Demo role")).toHaveCount(0);
  await expect(page.locator(".demo-banner")).toHaveCount(0);
  await page.reload();
  await expect(page.locator(".app-shell")).toBeVisible();
  await page.locator(".sidebar-bottom .profile").click();
  await expect(
    page.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
  expect((await page.request.get("/api/v1/auth/session")).status()).toBe(401);
});

test("charge creation posts versions and idempotency and survives reload", async ({
  page,
}) => {
  await login(page);
  await page
    .getByRole("link", { name: "Financial management", exact: true })
    .click();
  const description = "UI charge " + Date.now();
  await page
    .getByRole("button", { name: "Create charges", exact: true })
    .click();
  await page.getByLabel("DESCRIPTION", { exact: true }).fill(description);
  await page.getByLabel("Amount (TRY)", { exact: true }).fill("100");
  await page.getByRole("checkbox", { name: "A-12", exact: true }).check();
  const response = page.waitForResponse(
    (r) =>
      r.url().endsWith("/charge-batches") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  const saved = await response;
  expect(saved.status()).toBe(201);
  const headers = saved.request().headers();
  expect(headers["idempotency-key"]).toBeTruthy();
  expect(headers["x-csrf-token"]).toBeTruthy();
  expect(
    saved.request().postDataJSON().items[0].expectedAccountVersion,
  ).toEqual(expect.any(Number));
  await expect(page.getByRole("status")).toContainText(
    "financial transaction has been saved",
  );
  await page.reload();
  await expect(
    page.getByRole("row").filter({ hasText: description }),
  ).toBeVisible();
});

test("resident notifications upload a private receipt, persist and withdraw on the server", async ({
  page,
}) => {
  await login(page, "resident");
  await page
    .getByRole("link", { name: "Payment notifications", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Notify a payment", exact: true })
    .click();
  await page.getByLabel("Amount (TRY)", { exact: true }).fill("25");
  const reference = "UI transfer " + Date.now();
  await page
    .getByLabel("Bank reference / transfer description", { exact: true })
    .fill(reference);
  await page.locator("input[name=receipt]").setInputFiles({
    name: "receipt.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\nBrowser receipt\n%%EOF"),
  });
  const response = page.waitForResponse(
    (r) =>
      r.url().endsWith("/payment-notifications") &&
      r.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Send notification", exact: true })
    .click();
  const saved = await response;
  expect(saved.status()).toBe(201);
  expect(saved.request().postDataJSON().receiptId).toBeTruthy();
  await expect(page.getByRole("status")).toContainText(/notification/i);
  await page.reload();
  const row = page.locator(".payment-row").filter({ hasText: reference });
  await expect(row).toBeVisible();
  await row
    .getByRole("button", { name: "Withdraw notification", exact: true })
    .click();
  await expect(row).toContainText("Withdrawn");
  await expect(
    row.getByRole("button", { name: "Withdraw notification", exact: true }),
  ).toHaveCount(0);
});

test("request creation, comment and manager status are persistent HTTP workflows", async ({
  page,
  browser,
}) => {
  await login(page, "resident");
  await page
    .getByRole("link", { name: "Service requests", exact: false })
    .click();
  await page.getByRole("button", { name: "New request", exact: true }).click();
  const title = "Browser pipe " + Date.now();
  await page.getByLabel("Request title", { exact: true }).fill(title);
  await page.getByLabel("Location", { exact: true }).fill("Kitchen");
  await page
    .getByRole("button", { name: "Submit request", exact: true })
    .click();
  await expect(
    page.getByRole("link", { name: title, exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: title, exact: true }).click();
  await page
    .getByLabel("Your message", { exact: true })
    .fill("Please inspect the valve.");
  await page.getByRole("button", { name: "Send message", exact: true }).click();
  await expect(page.locator(".request-comment")).toContainText(
    "Please inspect the valve.",
  );
  const url = page.url();
  await page.reload();
  await expect(page.locator(".request-comment")).toContainText(
    "Please inspect the valve.",
  );
  const context = await browser.newContext();
  const manager = await context.newPage();
  await login(manager);
  await manager.goto(url);
  await manager
    .getByRole("combobox", { name: "Assigned to", exact: true })
    .selectOption("staff");
  await expect(manager.getByRole("status")).toHaveCount(0);
  await manager
    .getByRole("combobox", { name: "Request status", exact: true })
    .selectOption({ label: "In progress" });
  await expect(manager.locator(".request-detail-header")).toContainText(
    "In progress",
  );
  await page.reload();
  await expect(page.locator(".request-detail-header")).toContainText(
    "In progress",
  );
  await context.close();
});

test("a protected deep link is restored after sign in and residents cannot edit announcements", async ({
  page,
}) => {
  await login(page, "resident", "/requests/not-accessible");
  await expect(page).toHaveURL(/\/requests\/not-accessible$/);
  await expect(page.getByRole("alert")).toContainText(/unavailable|outside/i);
  await page.goto("/announcements/new");
  await expect(
    page.getByRole("heading", { name: "Access denied", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByLabel("Announcement title", { exact: true }),
  ).toHaveCount(0);
});

test("published and draft announcements use server audience authorization", async ({
  page,
  browser,
}) => {
  await login(page);
  await page.getByRole("link", { name: "Announcements", exact: true }).click();
  await page
    .getByRole("link", { name: "New announcement", exact: true })
    .click();
  const title = "Browser notice " + Date.now();
  await page.getByLabel("Announcement title", { exact: true }).fill(title);
  await page
    .getByLabel("Announcement content", { exact: true })
    .fill("Maintenance tomorrow.");
  await page
    .getByRole("button", { name: "Publish announcement", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: title, exact: true }),
  ).toBeVisible();
  const url = page.url();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: title, exact: true }),
  ).toBeVisible();
  const context = await browser.newContext();
  const resident = await context.newPage();
  await login(resident, "resident");
  await resident.goto(url);
  await expect(
    resident.getByRole("heading", { name: title, exact: true }),
  ).toBeVisible();
  await expect(
    resident.getByRole("link", { name: "Edit announcement", exact: true }),
  ).toHaveCount(0);
  await context.close();
});

test("community and own-unit selectors come from memberships and isolate account projections", async ({
  page,
}) => {
  await login(page, "resident");
  await page
    .getByRole("combobox", { name: "Community", exact: true })
    .selectOption("bosphorus-homes");
  await expect(page.locator(".workspace strong")).toHaveText("Bosphorus Homes");
  await expect(
    page.getByRole("combobox", { name: "My unit", exact: true }),
  ).toHaveValue("b06");
  await page.reload();
  await expect(
    page.getByRole("combobox", { name: "Community", exact: true }),
  ).toHaveValue("bosphorus-homes");
  await page.goto("/account/a12");
  await expect(
    page.getByRole("heading", { name: "Access denied", exact: true }),
  ).toBeVisible();
});

test("resident directory shows real household names without relationship or contact history", async ({
  page,
}) => {
  await login(page, "resident");
  await page
    .getByRole("link", { name: "Community directory", exact: true })
    .click();
  await expect(page.locator(".resident-directory")).toContainText(
    "Household Member",
  );
  await expect(page.locator(".resident-directory")).not.toContainText(
    "@govener.test",
  );
  await page.goto("/units/a12");
  await expect(page.locator(".resident-household")).toContainText(
    "Household Member",
  );
  await expect(
    page.getByRole("button", { name: "Owner & tenant history", exact: true }),
  ).toHaveCount(0);
});

test("a manager with limited permissions does not request or display financial data", async ({
  page,
}) => {
  const forbidden = [];
  page.on("response", (r) => {
    if (r.status() === 403 && r.url().includes("/api/"))
      forbidden.push(r.url());
  });
  await login(page, "staff");
  await expect(
    page.getByRole("link", { name: "Financial management", exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "Unit accounts", exact: true }),
  ).toHaveCount(0);
  await page.getByRole("link", { name: /Service requests/ }).click();
  await expect(
    page.getByRole("heading", { name: "Service requests", exact: true }),
  ).toBeVisible();
  expect(forbidden).toEqual([]);
});

test("401 on refresh clears visible community state and returns to real login", async ({
  page,
}) => {
  await login(page);
  await page.route("**/api/v1/communities/olive-gardens/context", (route) =>
    route.fulfill({
      status: 401,
      contentType: "application/problem+json",
      body: JSON.stringify({ code: "AUTHENTICATION_REQUIRED", status: 401 }),
    }),
  );
  await page
    .getByRole("button", { name: "Refresh information", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Sign in", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".app-shell")).toHaveCount(0);
  await expect(page.getByRole("alert")).toContainText("session has expired");
});

test("partial payment and reasoned reversal refresh server account totals and ledger", async ({
  page,
}) => {
  await login(page);
  const batch = await charge(page, 10000);
  const chargeId = batch.charges[0].id;
  const baseline = await account(page);
  await page.reload();
  await page
    .getByRole("link", { name: "Financial management", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Record payment", exact: true })
    .click();
  await page
    .locator(".financial-form-panel select")
    .first()
    .selectOption("a12");
  await page.getByLabel("Amount (TRY)", { exact: true }).fill("40");
  const reference = "Partial UI " + Date.now();
  await page
    .getByLabel("Bank reference / transfer description", { exact: true })
    .fill(reference);
  await page.locator(`[name="allocation-${chargeId}"]`).fill("40");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByRole("status")).toContainText(
    "financial transaction has been saved",
  );
  expect((await account(page)).paidMinor).toBe(baseline.paidMinor + 4000);
  const row = page.getByRole("row").filter({ hasText: reference });
  await row
    .getByRole("button", { name: "Reverse payment", exact: true })
    .click();
  await page
    .locator(".correction-panel textarea")
    .fill("Incorrect bank reference");
  await page.locator(".correction-panel .primary").click();
  await expect(row).toContainText("Reversed");
  expect((await account(page)).paidMinor).toBe(baseline.paidMinor);
  await page.goto("/account/a12");
  await page
    .getByRole("button", { name: "Account movements", exact: true })
    .click();
  await expect(
    page.getByRole("row").filter({ hasText: "Incorrect bank reference" }),
  ).toBeVisible();
});

test("unknown financial outcome retains the same idempotency key on explicit retry", async ({
  page,
}) => {
  await login(page);
  await page
    .getByRole("link", { name: "Financial management", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Create charges", exact: true })
    .click();
  await page
    .getByLabel("DESCRIPTION", { exact: true })
    .fill("Uncertain " + Date.now());
  await page.getByLabel("Amount (TRY)", { exact: true }).fill("15");
  await page.getByRole("checkbox", { name: "A-03", exact: true }).check();
  const keys = [];
  let first = true;
  await page.route(
    "**/api/v1/communities/olive-gardens/charge-batches",
    async (route) => {
      keys.push(route.request().headers()["idempotency-key"]);
      if (first) {
        first = false;
        await route.fetch();
        await route.abort("failed");
      } else await route.continue();
    },
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(
    "outcome could not be confirmed",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByRole("status")).toContainText(
    "financial transaction has been saved",
  );
  expect(keys).toHaveLength(2);
  expect(keys[0]).toBe(keys[1]);
});

test("profile writes and preference writes use independent server versions", async ({
  page,
}) => {
  await login(page, "neighbor");
  await page.getByRole("link", { name: "My profile", exact: true }).click();
  const name = "Updated Neighbor";
  await page.getByLabel("Full name", { exact: true }).fill(name);
  await page.getByRole("button", { name: "Save profile", exact: true }).click();
  await expect(page.locator(".sidebar-bottom .profile")).toContainText(name);
  await page
    .getByRole("button", { name: "Save preferences", exact: true })
    .click();
  await expect(page.locator(".inline-success")).toContainText("saved");
  await page.reload();
  await expect(page.getByLabel("Full name", { exact: true })).toHaveValue(name);
});

test("password reset uses the API and validates incomplete reset links", async ({
  page,
}) => {
  await page.addInitScript(() =>
    localStorage.setItem("govener.language", "en"),
  );
  await page.goto("/forgot-password");
  await page
    .getByLabel("Email address", { exact: true })
    .fill("missing@govener.test");
  const response = page.waitForResponse((r) =>
    r.url().endsWith("/password-reset-requests"),
  );
  await page
    .getByRole("button", { name: "Request reset", exact: true })
    .click();
  expect((await response).status()).toBe(202);
  await expect(page.getByRole("status")).toContainText("If an account exists");
  await page.goto("/reset-password");
  await page.getByLabel("Password", { exact: true }).fill("NewPassword123!");
  await page
    .getByLabel("Confirm password", { exact: true })
    .fill("NewPassword123!");
  await page
    .getByRole("button", { name: "Reset password", exact: true })
    .click();
  await expect(page.getByRole("alert")).toContainText("invalid or incomplete");
});
