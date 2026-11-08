const { randomUUID } = require("node:crypto");
const { expect } = require("@playwright/test");
const SITE = "/api/v1/communities/olive-gardens";
async function login(page, user = "manager", route = "/login") {
  const container = process.env.GOVENER_BROWSER_TEST_CONTAINER;
  if (!/^govener-browser-[a-f0-9]{8}$/.test(container ?? ""))
    throw new Error("An isolated test database is required");
  require("node:child_process").execFileSync(
    "docker",
    [
      "exec",
      container,
      "mongosh",
      "govener_browser",
      "--quiet",
      "--eval",
      "db.auth_attempts.deleteMany({})",
    ],
    { windowsHide: true, stdio: "ignore" },
  );
  await page.addInitScript(() =>
    localStorage.setItem("govener.language", "en"),
  );
  await page.goto(route);
  await page
    .getByLabel("Email address", { exact: true })
    .fill(user + "@govener.test");
  await page.getByLabel("Password", { exact: true }).fill("DemoPass123!");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.locator(".app-shell")).toBeVisible();
  if (
    await page.getByRole("combobox", { name: "Language", exact: true }).count()
  )
    await page
      .getByRole("combobox", { name: "Language", exact: true })
      .selectOption("en");
  if (
    route === "/login" &&
    (await page
      .getByRole("combobox", { name: "Community", exact: true })
      .inputValue()) !== "olive-gardens"
  )
    await page
      .getByRole("combobox", { name: "Community", exact: true })
      .selectOption("olive-gardens");
  await expect(page.locator(".app-shell")).toBeVisible();
}
async function command(page, path, body, key) {
  const session = await (await page.request.get("/api/v1/auth/session")).json();
  const response = await page.request.post(SITE + path, {
    data: body,
    headers: {
      "X-CSRF-Token": session.csrfToken,
      ...(key ? { "Idempotency-Key": key } : {}),
    },
  });
  expect(response.ok(), await response.text()).toBeTruthy();
  return response.status() === 204 ? null : response.json();
}
async function account(page, unit = "a12") {
  return (await page.request.get(SITE + "/unit-accounts/" + unit)).json();
}
async function charge(page, amount = 10000, unit = "a12") {
  const summary = await account(page, unit);
  const now = new Date();
  const month = now.toISOString().slice(0, 7);
  const end = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 0))
    .toISOString()
    .slice(0, 10);
  return command(
    page,
    "/charge-batches",
    {
      items: [
        {
          unitId: unit,
          description: "Browser assessment " + Date.now(),
          periodStart: month + "-01",
          periodEnd: end,
          dueOn: month + "-15",
          amountMinor: amount,
          currency: "TRY",
          expectedAccountVersion: summary.version,
        },
      ],
    },
    randomUUID(),
  );
}
module.exports = { login, command, account, charge, SITE };
