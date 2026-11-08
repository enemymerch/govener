package com.govener.e2e;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;

/** Validates actual HTTP response shapes against the FE-owned OpenAPI contract. */
final class ContractAssertions {
  private final JsonNode spec;

  ContractAssertions(ObjectMapper mapper) throws Exception {
    Path path = Path.of("../docs/openapi.json");
    if (!Files.exists(path)) path = Path.of("docs/openapi.json");
    spec = mapper.readTree(path.toFile());
  }

  void response(String method, String path, int status, JsonNode body) {
    String route = path.split("\\?")[0].replaceFirst("^/api/v1", "");
    JsonNode operation = null;
    for (var it = spec.path("paths").properties().iterator(); it.hasNext(); ) {
      var entry = it.next();
      String regex = entry.getKey().replaceAll("\\{[^}]+}", "[^/]+");
      if (route.matches(regex) && entry.getValue().has(method.toLowerCase(Locale.ROOT))) {
        operation = entry.getValue().get(method.toLowerCase(Locale.ROOT));
        break;
      }
    }
    assertThat(operation).as("Contract operation %s %s", method, route).isNotNull();
    assertThat(operation.path("responses").has(Integer.toString(status)))
        .as("Declared HTTP status %s for %s", status, route)
        .isTrue();
    if (status == 204) {
      assertThat(body).isNull();
      return;
    }
    JsonNode schema;
    if (status >= 400) schema = spec.path("components").path("schemas").path("Problem");
    else {
      var content = operation.path("responses").path(Integer.toString(status)).path("content");
      if (content.isMissingNode() || content.isEmpty()) {
        assertThat(body).isNull();
        return;
      }
      if (content.has("application/octet-stream")) return;
      schema = content.path("application/json").path("schema");
      assertThat(schema.isMissingNode()).as("Declared success status").isFalse();
    }
    validate(schema, body, "$");
  }

  private void validate(JsonNode schema, JsonNode body, String path) {
    final JsonNode resolved =
        schema.has("$ref") ? spec.at(schema.get("$ref").asText().substring(1)) : schema;
    validateResolved(resolved, body, path);
  }

  private void validateResolved(JsonNode schema, JsonNode body, String path) {
    assertThat(body).as(path).isNotNull();
    if (body.isNull()) {
      assertThat(schema.path("nullable").asBoolean()).as(path + " permits null").isTrue();
      return;
    }
    String type = schema.path("type").asText();
    switch (type) {
      case "object" -> {
        assertThat(body.isObject()).as(path).isTrue();
        for (var field : schema.path("required"))
          assertThat(body.has(field.asText())).as(path + " requires " + field.asText()).isTrue();
        if (schema.has("additionalProperties") && !schema.get("additionalProperties").asBoolean())
          body.fieldNames()
              .forEachRemaining(
                  name ->
                      assertThat(schema.path("properties").has(name))
                          .as(path + " unexpected " + name)
                          .isTrue());
        for (var it = body.properties().iterator(); it.hasNext(); ) {
          var field = it.next();
          if (schema.path("properties").has(field.getKey()))
            validate(
                schema.path("properties").get(field.getKey()),
                field.getValue(),
                path + "." + field.getKey());
        }
      }
      case "array" -> {
        assertThat(body.isArray()).as(path).isTrue();
        for (var item : body) validate(schema.get("items"), item, path + "[]");
      }
      case "integer" -> assertThat(body.isIntegralNumber()).as(path).isTrue();
      case "number" -> assertThat(body.isNumber()).as(path).isTrue();
      case "boolean" -> assertThat(body.isBoolean()).as(path).isTrue();
      case "string" -> {
        assertThat(body.isTextual()).as(path).isTrue();
        if (schema.path("format").asText().equals("date")) java.time.LocalDate.parse(body.asText());
        if (schema.path("format").asText().equals("date-time"))
          java.time.Instant.parse(body.asText());
      }
      default -> throw new AssertionError("Unsupported contract schema at " + path + ": " + schema);
    }
    if (schema.has("enum")) {
      List<String> values = new ArrayList<>();
      schema.get("enum").forEach(v -> values.add(v.asText()));
      assertThat(body.asText()).as(path).isIn(values);
    }
    if (schema.has("minimum"))
      assertThat(body.decimalValue())
          .as(path)
          .isGreaterThanOrEqualTo(schema.get("minimum").decimalValue());
    if (schema.has("maximum"))
      assertThat(body.decimalValue())
          .as(path)
          .isLessThanOrEqualTo(schema.get("maximum").decimalValue());
  }
}
