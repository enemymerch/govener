package com.govener.e2e;

import static com.govener.adapter.in.web.Views.map;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.govener.configuration.DemoData;
import com.govener.identity.adapter.out.mail.ResetTokenCipher;
import com.govener.identity.adapter.out.mongo.MongoResetEmailRepository;
import com.govener.notification.application.OutboxProjector;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class AcceptanceSteps {
  private static final String SITE = "/api/v1/communities/olive-gardens";
  @LocalServerPort int port;
  @Autowired ObjectMapper mapper;
  @Autowired MongoTemplate mongo;
  @Autowired DemoData seed;
  @Autowired OutboxProjector projector;
  @Autowired MongoResetEmailRepository resetEmails;
  @Autowired ResetTokenCipher resetCipher;
  private final Map<String, Client> clients = new HashMap<>();
  private ContractAssertions contract;
  private boolean verified;

  private record Result(int status, JsonNode body, String raw, HttpHeaders headers) {}

  private static final class Client {
    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final HttpClient http =
        HttpClient.newBuilder()
            .cookieHandler(cookies)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    String csrf;
  }

  @Before
  public void reset() throws Exception {
    for (String name : mongo.getCollectionNames()) mongo.remove(new Query(), name);
    seed.seed();
    clients.clear();
    verified = false;
    contract = new ContractAssertions(mapper);
  }

  private Result send(
      Client client, String method, String path, Object payload, String key, boolean includeCsrf)
      throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(30));
    if (includeCsrf && client.csrf != null) request.header("X-CSRF-Token", client.csrf);
    if (key != null) request.header("Idempotency-Key", key);
    HttpRequest.BodyPublisher body =
        payload == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload));
    if (payload != null) request.header("Content-Type", "application/json");
    var response =
        client.http.send(
            request.method(method, body).build(), HttpResponse.BodyHandlers.ofString());
    JsonNode json = response.body().isEmpty() ? null : mapper.readTree(response.body());
    contract.response(method, path, response.statusCode(), json);
    return new Result(response.statusCode(), json, response.body(), response.headers());
  }

  private Result call(
      String user, String method, String path, Object body, String key, int expected)
      throws Exception {
    Result result = send(client(user), method, path, body, key, true);
    assertThat(result.status()).as("%s %s: %s", method, path, result.raw()).isEqualTo(expected);
    return result;
  }

  private Client client(String user) throws Exception {
    if (clients.containsKey(user)) return clients.get(user);
    Client client = new Client();
    clients.put(user, client);
    if (!user.equals("anonymous")) login(client, user, DemoData.PASSWORD);
    return client;
  }

  private void login(Client client, String user, String password) throws Exception {
    Result csrf = send(client, "GET", "/api/v1/auth/csrf", null, null, false);
    assertThat(csrf.status()).isEqualTo(200);
    client.csrf = csrf.body().get("csrfToken").asText();
    Result login =
        send(
            client,
            "POST",
            "/api/v1/auth/login",
            map("email", user + "@govener.test", "password", password),
            null,
            true);
    assertThat(login.status()).as(login.raw()).isEqualTo(200);
    client.csrf = login.body().get("csrfToken").asText();
  }

  private Result get(String user, String path) throws Exception {
    return call(user, "GET", path, null, null, 200);
  }

  private Object chargeItem(String unit, long amount, long version, String start, String end) {
    return map(
        "unitId",
        unit,
        "description",
        "Monthly dues",
        "periodStart",
        start,
        "periodEnd",
        end,
        "dueOn",
        "2026-11-15",
        "amountMinor",
        amount,
        "currency",
        "TRY",
        "expectedAccountVersion",
        version);
  }

  private Result charge(String unit, long amount, long version) throws Exception {
    return call(
        "manager",
        "POST",
        SITE + "/charge-batches",
        map("items", List.of(chargeItem(unit, amount, version, "2026-11-01", "2026-11-30"))),
        UUID.randomUUID().toString(),
        201);
  }

  private String chargeId(Result batch) {
    return batch.body().get("charges").get(0).get("id").asText();
  }

  private Object payment(String unit, long amount, long version, List<?> allocations) {
    return map(
        "unitId",
        unit,
        "accountId",
        "olive-bank",
        "amountMinor",
        amount,
        "currency",
        "TRY",
        "paidOn",
        "2026-11-08",
        "reference",
        "Bank transfer",
        "allocations",
        allocations,
        "expectedAccountVersion",
        version);
  }

  private long amount(String user, String path, String field) throws Exception {
    return get(user, path).body().get(field).asLong();
  }

  private String id(Result result) {
    return result.body().get("id").asText();
  }

  private Result request(String user) throws Exception {
    return call(
        user,
        "POST",
        SITE + "/requests",
        map(
            "unitId",
            user.equals("neighbor") ? "a03" : "a12",
            "title",
            "Water leak",
            "location",
            "Kitchen",
            "description",
            "Please inspect the pipe.",
            "photoIds",
            List.of()),
        null,
        201);
  }

  private Object announcement(String status, String audience, List<String> ids) {
    return map(
        "title",
        "Water maintenance",
        "body",
        "Water will be interrupted tomorrow.",
        "category",
        "MAINTENANCE",
        "audience",
        map("type", audience, "ids", ids),
        "status",
        status);
  }

  private Result notifyTransfer(long amount) throws Exception {
    return call(
        "resident",
        "POST",
        SITE + "/payment-notifications",
        map(
            "unitId",
            "a12",
            "amountMinor",
            amount,
            "currency",
            "TRY",
            "paymentDate",
            "2026-11-08",
            "reference",
            "Transfer reference",
            "note",
            "Paid by bank"),
        null,
        201);
  }

  @Given("a community with a manager, residents and a separate community")
  public void fixtures() {
    assertThat(mongo.getCollection("memberships").countDocuments()).isEqualTo(5);
  }

  @When("the manager and resident browse their authorized community")
  public void browse() throws Exception {
    get("manager", "/api/v1/auth/session");
    get("manager", "/api/v1/me");
    get("manager", "/api/v1/me/preferences");
    assertThat(get("resident", "/api/v1/communities").body().get("items").size()).isEqualTo(1);
    get("resident", SITE + "/context");
    get("resident", SITE + "/dashboard");
    get("manager", SITE + "/dashboard?period=2026-11");
    get("resident", SITE + "/units?q=A&blockId=block-a");
    get("resident", SITE + "/units/a12");
    assertThat(get("manager", SITE + "/units/a12/relationships").body().get("items").size())
        .isEqualTo(1);
    get("manager", SITE + "/staff");
    get("manager", SITE + "/financial-accounts");
    get("manager", SITE + "/unit-accounts");
    verified = true;
  }

  @When("a resident attempts to cross community and manager boundaries")
  public void isolation() throws Exception {
    call("resident", "GET", "/api/v1/communities/bosphorus-homes/context", null, null, 404);
    call("resident", "GET", SITE + "/unit-accounts/a03", null, null, 404);
    call("resident", "GET", SITE + "/financial-accounts", null, null, 403);
    call("resident", "GET", SITE + "/expenses", null, null, 403);
    call("resident", "GET", SITE + "/units/a12/relationships", null, null, 403);
    call("anonymous", "GET", SITE + "/context", null, null, 401);
    verified = true;
  }

  @When("a resident opens the resident directory")
  public void directory() throws Exception {
    var own = get("resident", SITE + "/directory/units/a12");
    assertThat(own.body().get("residents").size()).isEqualTo(2);
    var all = get("resident", SITE + "/directory");
    assertThat(all.body().get("items").size()).isEqualTo(2);
    assertThat(all.raw()).doesNotContain("@govener.test", "phone", "TENANT", "OWNER");
    assertThat(get("resident", SITE + "/directory?mine=true").body().get("items").size())
        .isEqualTo(1);
    assertThat(get("resident", SITE + "/directory?q=Neighbor").body().get("items").size())
        .isEqualTo(1);
    verified = true;
  }

  @When("a financial command is retried with an idempotency key")
  public void idempotency() throws Exception {
    Object command = map("items", List.of(chargeItem("a12", 10000, 0, "2026-11-01", "2026-11-30")));
    Result first = call("manager", "POST", SITE + "/charge-batches", command, "same-key", 201);
    Result replay = call("manager", "POST", SITE + "/charge-batches", command, "same-key", 201);
    assertThat(replay.raw()).isEqualTo(first.raw());
    get("manager", SITE + "/charge-batches/" + id(first));
    call(
        "manager",
        "POST",
        SITE + "/charge-batches",
        map("items", List.of(chargeItem("a12", 20000, 0, "2026-11-01", "2026-11-30"))),
        "same-key",
        409);
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "chargedMinor")).isEqualTo(10000);
    assertThat(mongo.getCollection("charges").countDocuments()).isEqualTo(1);
    verified = true;
  }

  @When("the second item in a charge batch has a stale version")
  public void batchRollback() throws Exception {
    charge("a03", 20000, 0);
    call(
        "manager",
        "POST",
        SITE + "/charge-batches",
        map(
            "items",
            List.of(
                chargeItem("a12", 10000, 0, "2026-11-01", "2026-11-30"),
                chargeItem("a03", 10000, 0, "2026-11-01", "2026-11-30"))),
        "rollback-key",
        409);
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "chargedMinor")).isZero();
    assertThat(mongo.getCollection("charges").countDocuments()).isEqualTo(1);
    assertThat(mongo.getCollection("audit_records").countDocuments()).isEqualTo(1);
    verified = true;
  }

  @When("a partial payment is recorded and reversed")
  public void partialPayment() throws Exception {
    String charge = chargeId(charge("a12", 10000, 0));
    var paid =
        call(
            "manager",
            "POST",
            SITE + "/payments",
            payment("a12", 4000, 1, List.of(map("chargeId", charge, "amountMinor", 4000))),
            "partial-payment",
            201);
    assertThat(amount("resident", SITE + "/unit-accounts/a12", "outstandingMinor")).isEqualTo(6000);
    assertThat(get("resident", SITE + "/charges?status=PARTIALLY_PAID").body().get("items").size())
        .isEqualTo(1);
    get("resident", SITE + "/payments?unitId=a12");
    call(
        "manager",
        "POST",
        SITE + "/payments/" + id(paid) + "/reversals",
        map("reason", "Incorrect transfer", "expectedVersion", 0),
        "reverse-payment",
        201);
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "outstandingMinor")).isEqualTo(10000);
    assertThat(
            get("manager", SITE + "/financial-accounts")
                .body()
                .get("items")
                .get(0)
                .get("balanceMinor")
                .asLong())
        .isZero();
    assertThat(
            get("resident", SITE + "/unit-accounts/a12/entries?from=2026-11-01&to=2026-11-30")
                .body()
                .get("items")
                .size())
        .isEqualTo(3);
    verified = true;
  }

  @When("an overpayment leaves unallocated credit")
  public void overpayment() throws Exception {
    String charge = chargeId(charge("a12", 10000, 0));
    call(
        "manager",
        "POST",
        SITE + "/payments",
        payment("a12", 12000, 1, List.of(map("chargeId", charge, "amountMinor", 10000))),
        "overpayment",
        201);
    assertThat(amount("resident", SITE + "/unit-accounts/a12", "creditMinor")).isEqualTo(2000);
    assertThat(amount("resident", SITE + "/unit-accounts/a12", "outstandingMinor")).isZero();
    verified = true;
  }

  @When("a payment tries to allocate another unit's charge")
  public void invalidAllocation() throws Exception {
    String foreign = chargeId(charge("a03", 10000, 0));
    call(
        "manager",
        "POST",
        SITE + "/payments",
        payment("a12", 1000, 0, List.of(map("chargeId", foreign, "amountMinor", 1000))),
        "cross-unit",
        422);
    assertThat(mongo.getCollection("payments").countDocuments()).isZero();
    assertThat(
            get("manager", SITE + "/financial-accounts")
                .body()
                .get("items")
                .get(0)
                .get("balanceMinor")
                .asLong())
        .isZero();
    verified = true;
  }

  @When("two payments compete for the same account version")
  public void concurrentPayments() throws Exception {
    String charge = chargeId(charge("a12", 10000, 0));
    Client manager = client("manager");
    Object body = payment("a12", 7000, 1, List.of(map("chargeId", charge, "amountMinor", 7000)));
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(() -> send(manager, "POST", SITE + "/payments", body, "compete-one", true));
      var second =
          pool.submit(() -> send(manager, "POST", SITE + "/payments", body, "compete-two", true));
      assertThat(List.of(first.get().status(), second.get().status()))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "paidMinor")).isEqualTo(7000);
    assertThat(mongo.getCollection("payments").countDocuments()).isEqualTo(1);
    verified = true;
  }

  @When("a new tenant views retrospective and current charges")
  public void history() throws Exception {
    call(
        "manager",
        "POST",
        SITE + "/charge-batches",
        map("items", List.of(chargeItem("a12", 30000, 0, "2026-10-01", "2026-10-31"))),
        "old-charge",
        201);
    charge("a12", 10000, 1);
    assertThat(get("manager", SITE + "/charges?unitId=a12").body().get("items").size())
        .isEqualTo(2);
    assertThat(get("resident", SITE + "/charges?unitId=a12").body().get("items").size())
        .isEqualTo(1);
    assertThat(amount("resident", SITE + "/unit-accounts/a12", "chargedMinor")).isEqualTo(10000);
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "chargedMinor")).isEqualTo(40000);
    assertThat(
            get("resident", SITE + "/unit-accounts/a12/entries")
                .body()
                .get("items")
                .get(0)
                .get("balanceAfterMinor")
                .asLong())
        .isEqualTo(10000);
    verified = true;
  }

  @When("an unpaid charge is cancelled with a reason")
  public void cancelCharge() throws Exception {
    String charge = chargeId(charge("a12", 10000, 0));
    call(
        "manager",
        "POST",
        SITE + "/charges/" + charge + "/cancellations",
        map("reason", "Duplicate assessment", "expectedVersion", 0),
        "cancel",
        201);
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "chargedMinor")).isZero();
    assertThat(get("manager", SITE + "/charges?status=CANCELLED").body().get("items").size())
        .isEqualTo(1);
    verified = true;
  }

  @When("a transfer notification is reviewed and matched without double counting")
  public void transferReview() throws Exception {
    String notice = id(notifyTransfer(10000));
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "paidMinor")).isZero();
    call(
        "manager",
        "POST",
        SITE + "/payment-notifications/" + notice + "/reviews",
        map("decision", "REVIEWED", "note", "Verified statement", "expectedVersion", 0),
        null,
        201);
    String payment =
        id(
            call(
                "manager",
                "POST",
                SITE + "/payments",
                payment("a12", 10000, 0, List.of()),
                "unlinked-payment",
                201));
    call(
        "manager",
        "POST",
        SITE + "/payment-notifications/" + notice + "/payment-links",
        map("paymentId", payment, "expectedVersion", 1, "expectedPaymentVersion", 0),
        "link-existing",
        201);
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "paidMinor")).isEqualTo(10000);
    call(
        "manager",
        "POST",
        SITE + "/payments/" + payment + "/reversals",
        map("reason", "Wrong unit", "expectedVersion", 1),
        "reverse-linked",
        201);
    assertThat(
            get("resident", SITE + "/payment-notifications")
                .body()
                .get("items")
                .get(0)
                .get("status")
                .asText())
        .isEqualTo("REVIEWED");
    verified = true;
  }

  @When("a resident withdraws a pending transfer notification")
  public void withdraw() throws Exception {
    String notice = id(notifyTransfer(10000));
    call(
        "neighbor",
        "POST",
        SITE + "/payment-notifications/" + notice + "/withdrawals",
        map("expectedVersion", 0),
        null,
        404);
    call(
        "resident",
        "POST",
        SITE + "/payment-notifications/" + notice + "/withdrawals",
        map("expectedVersion", 0),
        null,
        201);
    assertThat(amount("manager", SITE + "/unit-accounts/a12", "paidMinor")).isZero();
    verified = true;
  }

  @When("an expense is paid in parts and a payment is reversed")
  public void expense() throws Exception {
    var expense =
        call(
            "manager",
            "POST",
            SITE + "/expenses",
            map(
                "description",
                "Elevator repair",
                "category",
                "MAINTENANCE",
                "vendor",
                "Repair Ltd",
                "incurredOn",
                "2026-11-08",
                "amountMinor",
                10000,
                "currency",
                "TRY"),
            "expense",
            201);
    String id = id(expense);
    var payment =
        call(
            "manager",
            "POST",
            SITE + "/expenses/" + id + "/payments",
            map(
                "accountId",
                "olive-bank",
                "paidOn",
                "2026-11-08",
                "amountMinor",
                4000,
                "expectedVersion",
                0),
            "expense-part",
            201);
    assertThat(amount("manager", SITE + "/expenses/" + id, "paidMinor")).isEqualTo(4000);
    call(
        "manager",
        "POST",
        SITE + "/expenses/" + id + "/payments",
        map(
            "accountId",
            "olive-bank",
            "paidOn",
            "2026-11-08",
            "amountMinor",
            7000,
            "expectedVersion",
            1),
        "expense-too-much",
        422);
    call(
        "manager",
        "POST",
        SITE + "/expense-payments/" + id(payment) + "/reversals",
        map("reason", "Wrong account", "expectedVersion", 0),
        "reverse-expense",
        201);
    assertThat(amount("manager", SITE + "/expenses/" + id, "paidMinor")).isZero();
    get("manager", SITE + "/expenses");
    get("manager", SITE + "/expense-payments?expenseId=" + id);
    verified = true;
  }

  @When("a resident and manager collaborate on a service request")
  public void serviceRequest() throws Exception {
    String id = id(request("resident"));
    call("neighbor", "GET", SITE + "/requests/" + id, null, null, 404);
    get("resident", SITE + "/requests?status=OPEN");
    get("manager", SITE + "/requests/" + id);
    call(
        "resident",
        "POST",
        SITE + "/requests/" + id + "/comments",
        map("body", "The leak is getting worse."),
        null,
        201);
    call(
        "manager",
        "POST",
        SITE + "/requests/" + id + "/comments",
        map("body", "A technician is on the way."),
        null,
        201);
    call(
        "manager",
        "POST",
        SITE + "/requests/" + id + "/assignments",
        map("assigneeId", "staff", "expectedVersion", 0),
        null,
        201);
    call(
        "manager",
        "POST",
        SITE + "/requests/" + id + "/status-changes",
        map("status", "IN_PROGRESS", "expectedVersion", 1),
        null,
        201);
    call(
        "manager",
        "POST",
        SITE + "/requests/" + id + "/status-changes",
        map("status", "RESOLVED", "expectedVersion", 1),
        null,
        409);
    call(
        "manager",
        "POST",
        SITE + "/requests/" + id + "/status-changes",
        map("status", "RESOLVED", "expectedVersion", 2),
        null,
        201);
    assertThat(get("resident", SITE + "/requests/" + id + "/comments").body().get("items").size())
        .isEqualTo(2);
    assertThat(get("resident", SITE + "/requests/" + id + "/events").body().get("items").size())
        .isEqualTo(4);
    verified = true;
  }

  @When("a manager publishes a draft to a selected unit")
  public void announcements() throws Exception {
    var draft =
        call(
            "manager",
            "POST",
            SITE + "/announcements",
            announcement("DRAFT", "UNITS", List.of("a12")),
            null,
            201);
    String id = id(draft);
    call("resident", "GET", SITE + "/announcements/" + id, null, null, 404);
    assertThat(get("resident", SITE + "/announcements").body().get("items").size()).isZero();
    @SuppressWarnings("unchecked")
    var publish = (Map<String, Object>) announcement("PUBLISHED", "UNITS", List.of("a12"));
    publish.put("expectedVersion", 0);
    call("manager", "PUT", SITE + "/announcements/" + id, publish, null, 200);
    get("resident", SITE + "/announcements/" + id);
    get("staff", SITE + "/announcements/" + id);
    call("neighbor", "GET", SITE + "/announcements/" + id, null, null, 404);
    verified = true;
  }

  @When("an outbox event becomes a private notification")
  public void notifications() throws Exception {
    call(
        "manager",
        "POST",
        SITE + "/announcements",
        announcement("PUBLISHED", "ALL", List.of()),
        null,
        201);
    projector.process();
    projector.process();
    var page = get("resident", SITE + "/notifications?unread=true");
    assertThat(page.body().get("items").size()).isEqualTo(1);
    String id = page.body().get("items").get(0).get("id").asText();
    call("neighbor", "PUT", SITE + "/notifications/" + id + "/read", null, null, 404);
    call("resident", "PUT", SITE + "/notifications/" + id + "/read", null, null, 204);
    assertThat(get("resident", SITE + "/notifications?unread=true").body().get("items").size())
        .isZero();
    call(
        "manager",
        "POST",
        SITE + "/announcements",
        announcement("PUBLISHED", "ALL", List.of()),
        null,
        201);
    projector.process();
    call(
        "resident",
        "POST",
        SITE + "/notifications/read-all",
        map("before", "2026-11-08T08:59:59Z"),
        null,
        204);
    assertThat(get("resident", SITE + "/notifications?unread=true").body().get("items").size())
        .isEqualTo(1);
    call(
        "resident",
        "POST",
        SITE + "/notifications/read-all",
        map("before", "2026-11-08T09:00:00Z"),
        null,
        204);
    assertThat(get("resident", SITE + "/notifications?unread=true").body().get("items").size())
        .isZero();
    verified = true;
  }

  @When("a user updates their profile and notification preferences")
  public void profile() throws Exception {
    call(
        "resident",
        "PATCH",
        "/api/v1/me",
        map(
            "name",
            "Updated Name",
            "email",
            "contact@example.com",
            "phone",
            "+905551112233",
            "expectedVersion",
            0),
        null,
        200);
    call(
        "resident",
        "PATCH",
        "/api/v1/me",
        map(
            "name",
            "Stale Name",
            "email",
            "contact@example.com",
            "phone",
            "",
            "expectedVersion",
            0),
        null,
        409);
    call(
        "resident",
        "PATCH",
        "/api/v1/me/preferences",
        map(
            "language",
            "en",
            "inApp",
            false,
            "push",
            false,
            "emailNotifications",
            false,
            "expectedVersion",
            1),
        null,
        200);
    call(
        "manager",
        "POST",
        SITE + "/announcements",
        announcement("PUBLISHED", "ALL", List.of()),
        null,
        201);
    projector.process();
    assertThat(get("resident", SITE + "/notifications").body().get("items").size()).isZero();
    verified = true;
  }

  @When("a pagination cursor is reused with another filter or user")
  public void pagination() throws Exception {
    String cursor = get("resident", SITE + "/directory?limit=1").body().get("nextCursor").asText();
    String encoded = URLEncoder.encode(cursor, StandardCharsets.UTF_8);
    assertThat(
            get("resident", SITE + "/directory?limit=1&cursor=" + encoded)
                .body()
                .get("items")
                .size())
        .isEqualTo(1);
    call("neighbor", "GET", SITE + "/directory?limit=1&cursor=" + encoded, null, null, 422);
    call(
        "resident",
        "GET",
        SITE + "/directory?limit=1&mine=true&cursor=" + encoded,
        null,
        null,
        422);
    call("resident", "GET", SITE + "/directory?limit=101", null, null, 422);
    verified = true;
  }

  @When("a request without CSRF or with unknown fields is submitted")
  public void validation() throws Exception {
    var client = client("resident");
    var body = map("name", "Name", "email", "mail@example.com", "phone", "", "expectedVersion", 0);
    assertThat(send(client, "PATCH", "/api/v1/me", body, null, false).status()).isEqualTo(403);
    body.put("roles", List.of("MANAGER"));
    call("resident", "PATCH", "/api/v1/me", body, null, 400);
    call(
        "resident",
        "PATCH",
        "/api/v1/me",
        map("name", "", "email", "invalid", "phone", "", "expectedVersion", 0),
        null,
        422);
    call("resident", "POST", "/api/v1/auth/logout", null, null, 204);
    call("resident", "GET", "/api/v1/auth/session", null, null, 401);
    verified = true;
  }

  @When("a password reset is consumed and then replayed")
  public void resetPassword() throws Exception {
    client("resident");
    call(
        "anonymous",
        "POST",
        "/api/v1/auth/password-reset-requests",
        map("email", "resident@govener.test"),
        null,
        202);
    String raw = resetCipher.decrypt(resetEmails.all().get(0).encryptedToken());
    assertThat(mongo.getCollection("password_resets").find().first().toJson()).doesNotContain(raw);
    call(
        "anonymous",
        "POST",
        "/api/v1/auth/password-resets",
        map("token", raw, "password", "NewPassword123!"),
        null,
        204);
    call("resident", "GET", "/api/v1/auth/session", null, null, 401);
    call(
        "anonymous",
        "POST",
        "/api/v1/auth/password-resets",
        map("token", raw, "password", "AnotherPassword123!"),
        null,
        422);
    login(new Client(), "resident", "NewPassword123!");
    verified = true;
  }

  @When("a private receipt is uploaded and attached to a transfer notification")
  public void attachment() throws Exception {
    var client = client("resident");
    String boundary = "GovenerBoundary";
    String multipart =
        "--"
            + boundary
            + "\r\nContent-Disposition: form-data; name=\"purpose\"\r\n\r\nPAYMENT_RECEIPT\r\n--"
            + boundary
            + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"receipt.pdf\"\r\nContent-Type: application/pdf\r\n\r\n%PDF-1.4\nreceipt\n%%EOF\r\n--"
            + boundary
            + "--\r\n";
    var response =
        client.http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + SITE + "/attachments"))
                .header("X-CSRF-Token", client.csrf)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(multipart))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    var uploaded = mapper.readTree(response.body());
    contract.response("POST", SITE + "/attachments", 201, uploaded);
    String id = uploaded.get("id").asText();
    get("resident", SITE + "/attachments/" + id);
    call("manager", "GET", SITE + "/attachments/" + id, null, null, 404);
    call(
        "resident",
        "POST",
        SITE + "/payment-notifications",
        map(
            "unitId",
            "a12",
            "amountMinor",
            10000,
            "currency",
            "TRY",
            "paymentDate",
            "2026-11-08",
            "reference",
            "Transfer",
            "note",
            "Receipt attached",
            "receiptId",
            id),
        null,
        201);
    get("manager", SITE + "/attachments/" + id);
    call("neighbor", "GET", SITE + "/attachments/" + id, null, null, 404);
    var download =
        client.http.send(
            HttpRequest.newBuilder(
                    URI.create(
                        "http://localhost:" + port + SITE + "/attachments/" + id + "/content"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(download.statusCode()).isEqualTo(200);
    assertThat(download.body()).startsWith("%PDF-");
    assertThat(download.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
    verified = true;
  }

  @When("an expense with immediate settlement references a foreign bank account")
  public void immediateExpense() throws Exception {
    var body =
        map(
            "description",
            "Repair",
            "category",
            "MAINTENANCE",
            "vendor",
            "Vendor",
            "incurredOn",
            "2026-11-08",
            "amountMinor",
            10000,
            "currency",
            "TRY",
            "immediatePayment",
            map("accountId", "bosphorus-bank", "paidOn", "2026-11-08"));
    call("manager", "POST", SITE + "/expenses", body, "invalid-immediate", 404);
    assertThat(mongo.getCollection("expenses").countDocuments()).isZero();
    assertThat(mongo.getCollection("expense_payments").countDocuments()).isZero();
    body.put("immediatePayment", map("accountId", "olive-bank", "paidOn", "2026-11-08"));
    var result = call("manager", "POST", SITE + "/expenses", body, "valid-immediate", 201);
    assertThat(result.body().get("paidMinor").asLong()).isEqualTo(10000);
    assertThat(
            get("manager", SITE + "/financial-accounts")
                .body()
                .get("items")
                .get(0)
                .get("balanceMinor")
                .asLong())
        .isEqualTo(-10000);
    verified = true;
  }

  @When("a recorded payment must exactly match its transfer notification")
  public void matchingTransfer() throws Exception {
    String notice = id(notifyTransfer(10000));
    @SuppressWarnings("unchecked")
    var wrong = (Map<String, Object>) payment("a12", 9000, 0, List.of());
    wrong.put("notificationId", notice);
    call("manager", "POST", SITE + "/payments", wrong, "mismatched-notice", 422);
    assertThat(mongo.getCollection("payments").countDocuments()).isZero();
    wrong.put("amountMinor", 10000);
    var paid = call("manager", "POST", SITE + "/payments", wrong, "matched-notice", 201);
    assertThat(
            get("resident", SITE + "/payment-notifications?status=RECORDED")
                .body()
                .get("items")
                .get(0)
                .get("paymentId")
                .asText())
        .isEqualTo(id(paid));
    assertThat(amount("resident", SITE + "/unit-accounts/a12", "paidMinor")).isEqualTo(10000);
    verified = true;
  }

  @When("a financial command omits its idempotency key or has an invalid date interval")
  public void malformedFinance() throws Exception {
    call(
        "manager",
        "POST",
        SITE + "/charge-batches",
        map("items", List.of(chargeItem("a12", 10000, 0, "2026-11-01", "2026-11-30"))),
        null,
        400);
    call(
        "manager",
        "GET",
        SITE + "/unit-accounts/a12/entries?from=2026-11-30&to=2026-11-01",
        null,
        null,
        422);
    call("manager", "GET", SITE + "/dashboard?period=2026-99", null, null, 422);
    assertThat(mongo.getCollection("charges").countDocuments()).isZero();
    verified = true;
  }

  @When("invalid credentials repeatedly attempt to sign in")
  public void rateLimit() throws Exception {
    Client client = new Client();
    var token = send(client, "GET", "/api/v1/auth/csrf", null, null, false);
    client.csrf = token.body().get("csrfToken").asText();
    for (int i = 0; i < 10; i++)
      assertThat(
              send(
                      client,
                      "POST",
                      "/api/v1/auth/login",
                      map("email", "unknown@govener.test", "password", "WrongPassword"),
                      null,
                      true)
                  .status())
          .isEqualTo(401);
    assertThat(
            send(
                    client,
                    "POST",
                    "/api/v1/auth/login",
                    map("email", "unknown@govener.test", "password", "WrongPassword"),
                    null,
                    true)
                .status())
        .isEqualTo(429);
    verified = true;
  }

  @Then("the workflow preserves its financial and access rules")
  public void success() {
    assertThat(verified).isTrue();
  }
}
