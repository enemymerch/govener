package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.govener.adapter.in.web.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

class EndpointContractTest {
  @Test
  void allContractOperationsHaveExactlyOneHttpHandler() throws Exception {
    Path path = Path.of("../docs/openapi.json");
    if (!Files.exists(path)) path = Path.of("docs/openapi.json");
    JsonNode spec = new ObjectMapper().readTree(path.toFile());
    Set<String> expected = new TreeSet<>();
    for (var entry : spec.get("paths").properties())
      for (var operation : entry.getValue().properties())
        expected.add(operation.getKey().toUpperCase(Locale.ROOT) + " /api/v1" + entry.getKey());
    List<String> actual = new ArrayList<>();
    for (Class<?> controller :
        List.of(
            AuthController.class,
            ProfileController.class,
            CommunityController.class,
            FinanceController.class,
            RequestController.class,
            AnnouncementController.class,
            NotificationController.class,
            AttachmentController.class)) {
      var root = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
      for (var method : controller.getDeclaredMethods()) {
        var mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping != null) {
          String suffix = mapping.value().length == 0 ? "" : mapping.value()[0];
          for (var verb : mapping.method()) actual.add(verb + " " + root.value()[0] + suffix);
        }
      }
    }
    assertThat(actual).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(expected);
    assertThat(actual).hasSize(59);
  }
}
