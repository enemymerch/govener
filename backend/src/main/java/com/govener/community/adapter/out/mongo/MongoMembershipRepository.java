package com.govener.community.adapter.out.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.community.application.CommunityPorts;
import com.govener.community.domain.Membership;
import com.govener.shared.adapter.out.mongo.MongoAggregateRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

public final class MongoMembershipRepository extends MongoAggregateRepository<Membership>
    implements CommunityPorts.Memberships {
  public MongoMembershipRepository(MongoTemplate mongo, ObjectMapper mapper) {
    super(mongo, mapper, "memberships", Membership.class);
  }

  public java.util.List<Membership> forUser(String userId) {
    return matching(
        org.springframework.data.mongodb.core.query.Query.query(
            org.springframework.data.mongodb.core.query.Criteria.where("data.userId").is(userId)));
  }

  public java.util.Optional<Membership> forUserInCommunity(String userId, String communityId) {
    return matching(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("data.userId")
                    .is(userId)
                    .and("communityId")
                    .is(communityId)))
        .stream()
        .findFirst();
  }
}
