package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.govener.attachment.adapter.out.validation.FileContentValidator;
import com.govener.communication.domain.*;
import com.govener.service.domain.*;
import com.govener.shared.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CollaborationDomainTest {
  @Test
  void audienceOnlyMatchesSelectedUnit() {
    var audience = new Audience(Audience.Type.UNITS, List.of("a12"));
    assertThat(audience.includes("a12", "a")).isTrue();
    assertThat(audience.includes("a03", "a")).isFalse();
  }

  @Test
  void audienceRejectsEmptyOrDuplicateSelection() {
    assertThatThrownBy(() -> new Audience(Audience.Type.UNITS, List.of()))
        .isInstanceOf(RuleViolation.class);
    assertThatThrownBy(() -> new Audience(Audience.Type.BLOCKS, List.of("a", "a")))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void requestHasCreatorScopeAndVersionedLifecycle() {
    var request =
        new ServiceRequest(
            "request",
            "site",
            1,
            null,
            "Leak",
            "Lobby",
            "",
            RequestStatus.OPEN,
            new Actor("creator", "Creator"),
            null,
            List.of(),
            Instant.now(),
            0);
    assertThat(request.visibleTo("neighbor", false)).isFalse();
    assertThat(request.visibleTo("creator", false)).isTrue();
    assertThat(
            request
                .assign(new Actor("staff", "Staff"))
                .changeStatus(RequestStatus.IN_PROGRESS)
                .version())
        .isEqualTo(2);
  }

  @Test
  void rejectsFakeImageAndTraversalName() {
    var validator = new FileContentValidator();
    assertThatThrownBy(
            () ->
                validator.validate(
                    "not-an-image".getBytes(), "photo.png", "image/png", "REQUEST_PHOTO"))
        .isInstanceOf(RuleViolation.class);
    assertThatThrownBy(
            () ->
                validator.validate(
                    "%PDF-1.4 %%EOF".getBytes(),
                    "../receipt.pdf",
                    "application/pdf",
                    "PAYMENT_RECEIPT"))
        .isInstanceOf(RuleViolation.class);
  }

  @Test
  void rejectsPdfAsRequestPhoto() {
    assertThatThrownBy(
            () ->
                new FileContentValidator()
                    .validate(
                        "%PDF-1.4 %%EOF".getBytes(),
                        "photo.pdf",
                        "application/pdf",
                        "REQUEST_PHOTO"))
        .isInstanceOf(RuleViolation.class);
  }
}
