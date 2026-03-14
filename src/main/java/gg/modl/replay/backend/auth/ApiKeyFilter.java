package gg.modl.replay.backend.auth;

import gg.modl.replay.backend.replay.model.ServerDocument;
import gg.modl.replay.backend.replay.repository.ServerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String ATTR_SERVER = "server";

    private final ServerRepository serverRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/plugin");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER_API_KEY);

        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing API key\"}");
            return;
        }

        ServerDocument server = serverRepository.findByApiKey(apiKey);
        if (server == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
            return;
        }

        request.setAttribute(ATTR_SERVER, server);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("PLUGIN"));
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(server.getId(), null, authorities);
        authentication.setDetails(server);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }
}
