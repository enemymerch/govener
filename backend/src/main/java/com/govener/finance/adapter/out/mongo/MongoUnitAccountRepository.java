package com.govener.finance.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.finance.application.FinancePorts;
import com.govener.finance.domain.UnitAccount;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoUnitAccountRepository extends MongoAggregateRepository<UnitAccount>
    implements FinancePorts.UnitAccounts {
  public MongoUnitAccountRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "unit_accounts", UnitAccount.class);
  }

  public java.util.Optional<UnitAccount> forUnit(String communityId, String unitId) {
    return matching(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("communityId")
                    .is(communityId)
                    .and("data.unitId")
                    .is(unitId)))
        .stream()
        .findFirst();
  }
}
