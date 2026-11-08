package com.govener.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govener.shared.domain.RuleViolation;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class ProblemResponses {
  private ProblemResponses() {}

  public static Map<String, Object> body(int status, String code, String detail, String path) {
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", "urn:govener:problem:" + code.toLowerCase(java.util.Locale.ROOT));
    problem.put("title", code);
    problem.put("status", status);
    problem.put("detail", detail);
    problem.put("instance", path);
    problem.put("code", code);
    problem.put("traceId", UUID.randomUUID().toString());
    return problem;
  }

  public static int status(RuleViolation failure) {
    return switch (failure.kind()) {
      case VALIDATION -> 422;
      case CONFLICT -> 409;
      case NOT_FOUND -> 404;
      case FORBIDDEN -> 403;
      case UNAUTHORIZED -> 401;
      case RATE_LIMITED -> 429;
    };
  }

  public static void write(
      ObjectMapper mapper,
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      String code,
      String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    mapper.writeValue(
        response.getOutputStream(), body(status, code, detail, request.getRequestURI()));
  }
}
