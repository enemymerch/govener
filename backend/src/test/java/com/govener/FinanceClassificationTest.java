package com.govener;

import static org.assertj.core.api.Assertions.*;

import com.govener.finance.domain.*;
import com.govener.shared.domain.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class FinanceClassificationTest {
  private Charge charge(String category) {
    return new Charge(
        "charge",
        "community",
        "unit",
        "Assessment",
        new BillingPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)),
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 11, 15),
        new Money(10000),
        0,
        null,
        "",
        0,
        category);
  }

  @Test
  void categoriesSurviveAllocationReleaseAndCancellation() {
    for (String category : new String[] {"DUES", "OTHER"}) {
      Charge original = charge(category);
      Charge changed = original.allocate(4000);
      assertThat(changed.category()).isEqualTo(category);
      assertThat(changed.outstandingMinor()).isEqualTo(6000);
      changed = changed.release(4000).cancel("Correction", Instant.now());
      assertThat(changed.category()).isEqualTo(category);
      assertThat(changed.outstandingMinor()).isZero();
    }
  }

  @Test
  void legacyRecordsRemainExplicitlyUnclassified() {
    assertThat(charge(null).category()).isEqualTo("UNSPECIFIED");
  }

  @Test
  void unsupportedCategoryIsRejected() {
    assertThatThrownBy(() -> charge("monthly dues")).isInstanceOf(RuleViolation.class);
  }

  @Test
  void bankMetadataSurvivesMoneyMovement() {
    FinancialAccount account =
        new FinancialAccount(
            "bank",
            "community",
            "Operating account",
            "BANK",
            0,
            5000,
            0,
            "Recorded bank",
            "TR000000000000000000000000");
    FinancialAccount changed = account.move(-1000);
    assertThat(changed.balanceMinor()).isEqualTo(4000);
    assertThat(changed.bankName()).isEqualTo(account.bankName());
    assertThat(changed.iban()).isEqualTo(account.iban());
  }

  @Test
  void legacyAccountsHaveEmptyMetadata() {
    FinancialAccount account =
        new FinancialAccount("bank", "community", "Operating account", "BANK", 0, 0, 0);
    assertThat(account.bankName()).isEmpty();
    assertThat(account.iban()).isEmpty();
  }
}
