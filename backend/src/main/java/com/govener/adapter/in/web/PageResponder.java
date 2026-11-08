package com.govener.adapter.in.web;

import com.govener.community.application.AccessService;
import com.govener.identity.adapter.in.security.CurrentActor;
import com.govener.shared.application.*;
import com.govener.shared.domain.Aggregate;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public final class PageResponder {
  private final Pagination pagination;
  private final CurrentActor current;
  private final AccessService access;
  private final SerializationPort serialization;

  public PageResponder(
      Pagination pagination,
      CurrentActor current,
      AccessService access,
      SerializationPort serialization) {
    this.pagination = pagination;
    this.current = current;
    this.access = access;
    this.serialization = serialization;
  }

  public <T> Map<String, Object> page(
      List<T> records,
      String communityId,
      String operation,
      Object filters,
      String cursor,
      int limit,
      Function<T, String> sortKey,
      Function<T, ?> view) {
    String userId = current.id();
    String scope =
        userId
            + ":"
            + communityId
            + ":"
            + operation
            + ":"
            + serialization.canonical(filters)
            + ":"
            + access.today();
    if (communityId != null) scope += ":" + access.membership(userId, communityId).version();
    return Views.page(pagination.page(records, scope, cursor, limit, sortKey), view);
  }

  public <T extends Aggregate> Map<String, Object> page(
      List<T> records,
      String communityId,
      String operation,
      Object filters,
      String cursor,
      int limit,
      Function<T, ?> view) {
    return page(records, communityId, operation, filters, cursor, limit, Aggregate::id, view);
  }
}
