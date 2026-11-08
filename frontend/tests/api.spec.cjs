const { test, expect } = require("@playwright/test");
const fs = require("node:fs");
const path = require("node:path");

test("OpenAPI covers all operations and specifies financial mutation guards", () => {
  const doc = JSON.parse(
    fs.readFileSync(path.join(__dirname, "../../docs/openapi.json"), "utf8"),
  );
  const operations = Object.values(doc.paths).flatMap((item) =>
    Object.values(item),
  );
  expect(operations).toHaveLength(59);
  expect(new Set(operations.map((item) => item.operationId)).size).toBe(59);
  for (const op of operations) {
    expect(op["x-access"]).toBeTruthy();
    expect(op.responses["409"]).toBeTruthy();
  }
  const payment = doc.paths["/communities/{communityId}/payments"].post;
  expect(
    payment.parameters.some(
      (item) => item.name === "Idempotency-Key" && item.required,
    ),
  ).toBe(true);
  expect(
    payment.parameters.some(
      (item) => item.name === "X-CSRF-Token" && item.required,
    ),
  ).toBe(true);
  expect(doc.components.schemas.PaymentCreate.required).toContain(
    "expectedAccountVersion",
  );
});

test("typed client sends cookies, CSRF, idempotency and encoded queries", async ({
  page,
}) => {
  await page.goto("/");
  const result = await page.evaluate(async () => {
    const { createApiClient } = await import("/src/api/client.ts");
    const calls = [];
    const client = createApiClient({
      fetcher: async (url, options) => {
        calls.push({
          url,
          method: options.method,
          headers: options.headers,
          credentials: options.credentials,
          body: options.body,
        });
        return new Response(
          JSON.stringify(
            url.endsWith("/auth/csrf")
              ? { csrfToken: "csrf-test" }
              : { id: "result" },
          ),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      },
    });
    await client.execute("getCsrf", {});
    await client.execute("recordPayment", {
      params: { communityId: "olive-gardens" },
      idempotencyKey: "stable-key",
      body: {
        unitId: "a-12",
        accountId: "bank",
        amountMinor: 100,
        currency: "TRY",
        paidOn: "2026-11-08",
        reference: "TEST",
        allocations: [],
        expectedAccountVersion: 0,
      },
    });
    await client.execute("listDirectory", {
      params: { communityId: "olive-gardens" },
      query: { q: "Ayşe & Bora", mine: true, limit: 20 },
    });
    return calls;
  });
  expect(result[1].credentials).toBe("include");
  expect(result[1].headers["X-CSRF-Token"]).toBe("csrf-test");
  expect(result[1].headers["Idempotency-Key"]).toBe("stable-key");
  expect(result[1].headers["Content-Type"]).toBe("application/json");
  expect(result[2].url).toContain("q=Ay%C5%9Fe+%26+Bora");
});

test("typed client handles empty responses, conflicts, aborts and 401 without retrying mutations", async ({
  page,
}) => {
  await page.goto("/");
  const result = await page.evaluate(async () => {
    const { createApiClient } = await import("/src/api/client.ts");
    let unauthorized = 0;
    let writes = 0;
    const client = createApiClient({
      onUnauthorized: () => unauthorized++,
      fetcher: async (url) => {
        writes++;
        return new Response(
          JSON.stringify({
            type: "about:blank",
            title: "Conflict",
            status: url.includes("session") ? 401 : 409,
            code: "VERSION_CONFLICT",
          }),
          {
            status: url.includes("session") ? 401 : 409,
            headers: { "Content-Type": "application/problem+json" },
          },
        );
      },
    });
    client.setCsrfToken("token");
    const errors = [];
    try {
      await client.execute("getSession", {});
    } catch (error) {
      errors.push(error.status);
    }
    try {
      await client.execute("reversePayment", {
        params: { communityId: "olive-gardens", paymentId: "p-1" },
        idempotencyKey: "same-key",
        body: { reason: "Test", expectedVersion: 0 },
      });
    } catch (error) {
      errors.push(error.code);
    }
    try {
      await client.execute("getContext", { params: { communityId: ".." } });
    } catch (error) {
      errors.push(error.code);
    }
    const empty = createApiClient({
      fetcher: async () => new Response(null, { status: 204 }),
    });
    empty.setCsrfToken("token");
    const noBody = await empty.execute("logout", {});
    const timed = createApiClient({
      timeoutMs: 5,
      fetcher: async (_, options) =>
        new Promise((_, reject) =>
          options.signal.addEventListener("abort", () =>
            reject(new DOMException("Aborted", "AbortError")),
          ),
        ),
    });
    try {
      await timed.execute("getSession", {});
    } catch (error) {
      errors.push(error.code);
    }
    return { errors, unauthorized, writes, noBody: noBody === undefined };
  });
  expect(result).toEqual({
    errors: [401, "VERSION_CONFLICT", "INVALID_PATH_PARAMETER", "TIMEOUT"],
    unauthorized: 1,
    writes: 2,
    noBody: true,
  });
});

test("monetary and receipt inputs reject unsupported values before submission", async ({
  page,
}) => {
  await page.goto("/");
  const result = await page.evaluate(async () => {
    const { parseAmountMinor, validReceipt } =
      await import("/src/features/payments/model.ts");
    return {
      valid: [
        parseAmountMinor("12,34"),
        parseAmountMinor("12.34"),
        parseAmountMinor("0.01"),
      ],
      invalid: ["0", "-1", "1e3", "1.001", "99999999", "1,2,3"].map(
        parseAmountMinor,
      ),
      file: validReceipt(
        new File(["data"], "receipt.exe", { type: "application/pdf" }),
      ),
    };
  });
  expect(result).toEqual({
    valid: [1234, 1234, 1],
    invalid: [null, null, null, null, null, null],
    file: false,
  });
});

test("prepared request repository uploads files before creating the parent and adapts opaque IDs", async ({
  page,
}) => {
  await page.goto("/");
  const result = await page.evaluate(async () => {
    const { createApiClient } = await import("/src/api/client.ts");
    const { createRepositories } = await import("/src/api/repositories.ts");
    const calls = [];
    const client = createApiClient({
      fetcher: async (url, options) => {
        const upload = url.endsWith("/attachments");
        calls.push({
          upload,
          hasContentType: !!options.headers["Content-Type"],
          purpose: upload ? options.body.get("purpose") : undefined,
          body: upload ? undefined : JSON.parse(options.body),
        });
        const payload = upload
          ? {
              id: "file-1",
              fileName: "photo.png",
              mediaType: "image/png",
              sizeBytes: 10,
              purpose: "REQUEST_PHOTO",
              contentUrl: "/ignored-external-url",
              createdAt: "2026-11-08T06:00:00Z",
            }
          : {
              id: "opaque-request-uuid",
              displayNumber: 8001,
              version: 3,
              communityId: "olive-gardens",
              unitId: "a-12",
              title: "Overview",
              location: "Entrance",
              description: "User content",
              status: "OPEN",
              creator: { id: "person-1", name: "Alex" },
              photos: [
                { id: "file-1", fileName: "photo.png", mediaType: "image/png" },
              ],
              comments: [],
              history: [],
              createdAt: "2026-11-08T06:00:00Z",
            };
        return new Response(JSON.stringify(payload), {
          status: 201,
          headers: { "Content-Type": "application/json" },
        });
      },
    });
    client.setCsrfToken("csrf");
    const request = await createRepositories(client).requests.create(
      "olive-gardens",
      "a-12",
      {
        title: "Overview",
        location: "Entrance",
        description: "User content",
        photos: [new File(["image"], "photo.png", { type: "image/png" })],
      },
    );
    return { calls, request };
  });
  expect(result.calls[0].upload).toBe(true);
  expect(result.calls[0].hasContentType).toBe(false);
  expect(result.calls[0].purpose).toBe("REQUEST_PHOTO");
  expect(result.calls[1].body.photoIds).toEqual(["file-1"]);
  expect(result.calls[1].body).not.toHaveProperty("creatorId");
  expect(result.request).toMatchObject({
    id: "opaque-request-uuid",
    displayNumber: 8001,
    version: 3,
    custom: true,
    status: "Open",
  });
  expect(result.request.photos[0]).toEqual({
    id: "file-1",
    name: "photo.png",
    mediaType: "image/png",
  });
});
