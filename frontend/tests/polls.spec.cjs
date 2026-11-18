const { test, expect } = require("@playwright/test");
const { login, SITE } = require("./support/helpers.cjs");
const { execFileSync } = require("node:child_process");

async function create(page, title, audience = "RESIDENTS", draft = false) {
  await page.goto("/polls");
  await page.getByRole("button", { name: "Create poll", exact: true }).click();
  await page.getByLabel("Poll title", { exact: true }).fill(title);
  await page
    .getByLabel("Description", { exact: true })
    .fill("Community choice");
  await page
    .getByRole("combobox", { name: "Who can vote?", exact: true })
    .selectOption(audience);
  await page.getByLabel("Option 1", { exact: true }).fill("Yes");
  await page.getByLabel("Option 2", { exact: true }).fill("No");
  const created = page.waitForResponse(
    (r) => r.url().endsWith("/polls") && r.request().method() === "POST",
  );
  await page
    .locator(".poll-editor")
    .getByRole("button", {
      name: draft ? "Save draft" : "Publish poll",
      exact: true,
    })
    .click();
  const response = await created;
  expect(response.status()).toBe(201);
  await expect(page.locator(".poll-detail")).toContainText(title);
  return response.json();
}
async function open(page, title) {
  await page.goto("/polls");
  await page
    .locator(".poll-list article")
    .filter({ hasText: title })
    .getByRole("button", { name: "View poll", exact: true })
    .click();
  await expect(page.locator(".poll-detail")).toContainText(title);
}
function fixture(script) {
  const container = process.env.GOVENER_BROWSER_TEST_CONTAINER;
  if (!/^govener-browser-[a-f0-9]{8}$/.test(container || ""))
    throw new Error("Disposable test database required");
  execFileSync(
    "docker",
    [
      "exec",
      container,
      "mongosh",
      "govener_browser",
      "--quiet",
      "--eval",
      script,
    ],
    { windowsHide: true, stdio: "ignore" },
  );
}

test("manager publishes a draft, resident votes, live results show identities and closure prevents votes", async ({
  page,
  browser,
}) => {
  await login(page);
  const title = "Garden vote " + Date.now();
  const poll = await create(page, title, "RESIDENTS", true);
  await expect(page.locator(".poll-detail")).toContainText("Draft");
  const resident = await browser.newPage({ baseURL: "http://127.0.0.1:4175" });
  await login(resident, "resident");
  await resident.goto("/polls");
  await expect(resident.locator(".poll-list")).not.toContainText(title);
  await page.getByRole("button", { name: "Publish poll", exact: true }).click();
  await expect(page.locator(".poll-detail")).toContainText("Open");
  await open(resident, title);
  await expect(resident.locator(".poll-results")).toContainText("0 votes");
  await resident.getByRole("checkbox", { name: "A-12", exact: true }).check();
  await resident.getByRole("radio", { name: "Yes", exact: true }).check();
  await resident
    .getByRole("button", { name: "Submit 1 unit votes", exact: true })
    .click();
  await expect(resident.locator(".poll-ballots")).toContainText(
    "Deniz Resident",
  );
  await expect(page.locator(".poll-ballots")).toContainText("Deniz Resident", {
    timeout: 15000,
  });
  await expect(page.locator(".poll-ballots")).toContainText("A-12");
  await page.getByRole("button", { name: "Close poll", exact: true }).click();
  await expect(page.locator(".poll-detail")).toContainText("Closed");
  await resident.reload();
  await open(resident, title);
  await expect(resident.locator(".poll-voting")).toHaveCount(0);
  const session = await (
    await resident.request.get("/api/v1/auth/session")
  ).json();
  const closed = await (
    await resident.request.get(SITE + "/polls/" + poll.id)
  ).json();
  const rejected = await resident.request.post(
    SITE + "/polls/" + poll.id + "/votes",
    {
      data: {
        optionId: poll.options[0].id,
        unitIds: ["a12"],
        expectedVersion: closed.version,
      },
      headers: { "X-CSRF-Token": session.csrfToken },
    },
  );
  expect(rejected.status()).toBe(409);
  await resident.close();
});

test("the first resident vote locks the unit for other occupants", async ({
  page,
  browser,
}) => {
  fixture(
    "db.residences.updateOne({_id:'poll-shared-home'},{$set:{communityId:'olive-gardens',version:0,data:{id:'poll-shared-home',communityId:'olive-gardens',unitId:'a12',personId:'neighbor-person',startsOn:'2026-11-01',endsOn:null,version:0}}},{upsert:true})",
  );
  await login(page);
  const title = "First unit vote " + Date.now();
  await create(page, title);
  const resident = await browser.newPage({ baseURL: "http://127.0.0.1:4175" });
  await login(resident, "resident");
  await open(resident, title);
  await resident.getByRole("checkbox", { name: "A-12", exact: true }).check();
  await resident.getByRole("radio", { name: "No", exact: true }).check();
  await resident
    .getByRole("button", { name: "Submit 1 unit votes", exact: true })
    .click();
  await expect(resident.locator(".poll-ballots tbody tr")).toHaveCount(1);
  await resident.close();
  const neighbor = await browser.newPage({ baseURL: "http://127.0.0.1:4175" });
  await login(neighbor, "neighbor");
  await open(neighbor, title);
  await expect(neighbor.getByRole("checkbox", { name: /A-12/ })).toBeDisabled();
  await expect(neighbor.locator(".poll-ballots")).toContainText(
    "Deniz Resident",
  );
  await neighbor.close();
});

test("an owner can vote for three owned units in one submission and see the persisted weighted result", async ({
  page,
  browser,
}) => {
  fixture(`
 db.units.updateOne({_id:'poll-owned-extra'},{$set:{communityId:'olive-gardens',version:0,data:{id:'poll-owned-extra',communityId:'olive-gardens',label:'A-04',blockId:'block-a',blockLabel:'Block A',floor:1,kind:'APARTMENT',areaSqm:90,version:0}}},{upsert:true});
 for(const unit of ['a12','a03','poll-owned-extra']){const id='poll-owner-'+unit;db.relationships.updateOne({_id:id},{$set:{communityId:'olive-gardens',version:0,data:{id,communityId:'olive-gardens',unitId:unit,personId:'resident-person',type:'OWNER',startsOn:'2026-11-01',endsOn:null,version:0}}},{upsert:true});}
 `);
  await login(page);
  const title = "Owner choice " + Date.now();
  await create(page, title, "OWNERS");
  const owner = await browser.newPage({ baseURL: "http://127.0.0.1:4175" });
  await login(owner, "resident");
  await open(owner, title);
  for (const name of ["A-12", "A-03", "A-04"])
    await owner.getByRole("checkbox", { name, exact: true }).check();
  await owner.getByRole("radio", { name: "Yes", exact: true }).check();
  await owner
    .getByRole("button", { name: "Submit 3 unit votes", exact: true })
    .click();
  await expect(owner.locator(".poll-ballots tbody tr")).toHaveCount(3);
  await expect(owner.locator(".poll-results")).toContainText("3 votes");
  await owner.reload();
  await open(owner, title);
  await expect(owner.locator(".poll-ballots tbody tr")).toHaveCount(3);
  await owner.close();
});

test("limited managers cannot create polls and resident mobile UI supports Turkish without overflow", async ({
  page,
}, testInfo) => {
  await login(page, "staff");
  await page.goto("/polls");
  await expect(
    page.getByRole("button", { name: "Create poll", exact: true }),
  ).toHaveCount(0);
  await page.context().clearCookies();
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, "resident");
  await page.goto("/polls");
  await expect(page.locator(".poll-list article").first()).toBeVisible();
  await page
    .locator(".poll-list article")
    .first()
    .getByRole("button", { name: "View poll", exact: true })
    .click();
  await page
    .getByRole("combobox", { name: "Language", exact: true })
    .selectOption("tr");
  await expect(
    page.getByRole("heading", { name: "Anketler", exact: true }).first(),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth - window.innerWidth,
    ),
  ).toBeLessThanOrEqual(1);
  await page.screenshot({
    path: testInfo.outputPath("poll-mobile.png"),
    fullPage: true,
  });
});
