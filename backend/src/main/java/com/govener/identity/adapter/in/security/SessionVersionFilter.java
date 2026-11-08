package com.govener.identity.adapter.in.security;

import com.govener.identity.application.IdentityService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class SessionVersionFilter extends OncePerRequestFilter {
  private final IdentityService identity;

  public SessionVersionFilter(IdentityService identity) {
    this.identity = identity;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof SessionPrincipal principal) {
      boolean valid;
      try {
        valid = identity.user(principal.id()).securityVersion() == principal.securityVersion();
      } catch (com.govener.shared.domain.RuleViolation failure) {
        valid = false;
      }
      if (!valid) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
      }
    }
    chain.doFilter(request, response);
  }
}
