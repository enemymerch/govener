package com.govener.e2e;

import static com.govener.adapter.in.web.Views.map;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.govener.configuration.DemoData;
import io.cucumber.java.en.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;

public class AdministrationSteps {
  @LocalServerPort int port;
  @Autowired ObjectMapper mapper;
  @Autowired MongoTemplate mongo;
  private boolean verified;

  private record Client(HttpClient http, String csrf) {}

  private record Result(int status, JsonNode body, String raw) {}

  private Result send(Client client, String method, String path, Object body, int expected)
      throws Exception {
    var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
    if (client.csrf() != null) builder.header("X-CSRF-Token", client.csrf());
    if (body != null) builder.header("Content-Type", "application/json");
    var result =
        client
            .http()
            .send(
                builder
                    .method(
                        method,
                        body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(result.statusCode())
        .as("%s %s: %s", method, path, result.body())
        .isEqualTo(expected);
    return new Result(
        result.statusCode(),
        result.body().isEmpty() ? null : mapper.readTree(result.body()),
        result.body());
  }

  private Client login(String email, String password) throws Exception {
    var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    var client = new Client(HttpClient.newBuilder().cookieHandler(cookies).build(), null);
    var token = send(client, "GET", "/auth/csrf", null, 200).body().path("csrfToken").asText();
    client = new Client(client.http(), token);
    var auth = send(client, "POST", "/auth/login", map("email", email, "password", password), 200);
    return new Client(client.http(), auth.body().path("csrfToken").asText());
  }

  private Client login(String id) throws Exception {
    return login(id + "@govener.test", DemoData.PASSWORD);
  }

  private Object unit(String label) {
    return map(
        "label",
        label,
        "blockId",
        "block-a",
        "blockLabel",
        "Block A",
        "floor",
        2,
        "kind",
        "APARTMENT",
        "areaSqm",
        95);
  }

  @When("administrative scope boundaries are exercised")
  public void boundaries() throws Exception {
    var admin = login("admin");
    assertThat(send(admin, "GET", "/admin/communities", null, 200).body().path("items")).hasSize(1);
    send(admin, "GET", "/admin/communities/bosphorus-homes/units", null, 404);
    send(admin, "POST", "/admin/communities/bosphorus-homes/units", unit("X-1"), 404);
    send(admin, "GET", "/admin/administrators", null, 403);
    send(
        admin,
        "POST",
        "/admin/administrators",
        map(
            "name",
            "Escalation",
            "loginEmail",
            "escalation@govener.test",
            "phone",
            "",
            "password",
            DemoData.PASSWORD,
            "communityIds",
            List.of()),
        403);
    send(
        admin,
        "POST",
        "/admin/communities",
        map("name", "Unauthorized", "address", "Istanbul"),
        403);
    send(login("manager"), "GET", "/admin/session", null, 403);
    send(login("resident"), "GET", "/admin/communities", null, 403);
    send(admin, "POST", "/admin/communities/olive-gardens/units", unit("A-99"), 201);
    verified = true;
  }

  @Then("only assigned communities and allowed administrative actions succeed")
  public void scopeVerified() {
    assertThat(verified).isTrue();
  }

  @When("root provisions an administrator and changes its assignments")
  public void rootProvision() throws Exception {
    var root = login("root");
    String cid =
        send(
                root,
                "POST",
                "/admin/communities",
                map("name", "New Community", "address", "Istanbul"),
                201)
            .body()
            .path("id")
            .asText();
    assertThat(
            mongo
                .getCollection("financial_accounts")
                .countDocuments(new Document("communityId", cid)))
        .isEqualTo(2);
    var created =
        send(
            root,
            "POST",
            "/admin/administrators",
            map(
                "name",
                "New Admin",
                "loginEmail",
                "newadmin@govener.test",
                "phone",
                "",
                "password",
                DemoData.PASSWORD,
                "communityIds",
                List.of(cid)),
            201);
    assertThat(created.raw()).doesNotContain("password");
    String id = created.body().path("id").asText();
    var admin = login("newadmin");
    send(admin, "POST", "/admin/communities/" + cid + "/units", unit("N-1"), 201);
    send(
        root,
        "PUT",
        "/admin/administrators/" + id + "/communities",
        map("communityIds", List.of(), "expectedVersion", 0),
        200);
    send(admin, "GET", "/admin/communities/" + cid + "/units", null, 404);
    send(
        root,
        "PUT",
        "/admin/administrators/" + id + "/communities",
        map("communityIds", List.of(cid), "expectedVersion", 0),
        409);
    send(
        root,
        "PUT",
        "/admin/administrators/root/communities",
        map("communityIds", List.of(cid), "expectedVersion", 0),
        403);
    verified = true;
  }

  @Then("the administrator immediately follows the current assignments")
  public void assignmentsVerified() {
    assertThat(verified).isTrue();
  }

  private Map<String, Object> resident(String email, String unit) {
    return map(
        "name",
        "New Resident",
        "loginEmail",
        email,
        "phone",
        "",
        "password",
        DemoData.PASSWORD,
        "role",
        "RESIDENT",
        "permissions",
        List.of("COMMUNITY_READ", "UNIT_READ", "DIRECTORY_READ", "FINANCE_READ"),
        "unitId",
        unit,
        "relationshipType",
        "TENANT",
        "startsOn",
        "2026-11-01",
        "endsOn",
        null,
        "livesHere",
        true);
  }

  @When("an administrator provisions a resident with a dated tenancy")
  public void residentProvision() throws Exception {
    var admin = login("admin");
    var result =
        send(
            admin,
            "POST",
            "/admin/communities/olive-gardens/users",
            resident("newresident@govener.test", "a12"),
            201);
    String id = result.body().path("id").asText();
    assertThat(result.raw()).doesNotContain("password");
    var membership =
        mongo.getCollection("memberships").find(new Document("data.userId", id)).first();
    assertThat(membership).isNotNull();
    String personId = membership.get("data", Document.class).getString("personId");
    assertThat(
            mongo
                .getCollection("relationships")
                .countDocuments(
                    new Document("data.personId", personId).append("data.startsOn", "2026-11-01")))
        .isEqualTo(1);
    assertThat(
            mongo
                .getCollection("residences")
                .countDocuments(new Document("data.personId", personId)))
        .isEqualTo(1);
    var resident = login("newresident");
    send(resident, "GET", "/communities/olive-gardens/units/a12", null, 200);
    send(resident, "GET", "/communities/bosphorus-homes/context", null, 404);
    send(resident, "GET", "/admin/session", null, 403);
    verified = true;
  }

  @Then("the new resident can sign in and access only its assigned unit")
  public void residentVerified() {
    assertThat(verified).isTrue();
  }

  @When("invalid user provisioning and stale unit updates are attempted")
  public void invalidProvision() throws Exception {
    var admin = login("admin");
    long before = mongo.getCollection("users").countDocuments();
    send(
        admin,
        "POST",
        "/admin/communities/olive-gardens/users",
        resident("invalid@govener.test", "b06"),
        404);
    assertThat(mongo.getCollection("users").countDocuments()).isEqualTo(before);
    var invalid = resident("invalid@govener.test", "a12");
    invalid.put("relationshipType", null);
    send(admin, "POST", "/admin/communities/olive-gardens/users", invalid, 422);
    assertThat(mongo.getCollection("users").countDocuments()).isEqualTo(before);
    var elevated = resident("invalid@govener.test", "a12");
    elevated.put("role", "ROOT");
    send(admin, "POST", "/admin/communities/olive-gardens/users", elevated, 400);
    var update = (Map<String, Object>) unit("A-12 renamed");
    update.put("expectedVersion", 0);
    send(admin, "PUT", "/admin/communities/olive-gardens/units/a12", update, 200);
    send(admin, "PUT", "/admin/communities/olive-gardens/units/a12", update, 409);
    send(admin, "POST", "/admin/communities/olive-gardens/units", unit("A-12 renamed"), 409);
    verified = true;
  }

  @Then("user creation is atomic and unit versions are protected")
  public void atomicVerified() {
    assertThat(verified).isTrue();
  }
}
