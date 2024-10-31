package com.isoft.gatewayserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Configuration
public class AppConfig {
    private static final String CORRELATION_ID_KEY = "correlation-id";
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static void addHeader(ServerWebExchange exchange, String corrId, boolean isRequest) {
        if (isRequest)
            exchange
                    .mutate()
                    .request(exchange.getRequest()
                            .mutate()
                            .header(CORRELATION_ID_KEY, corrId)
                            .build())
                    .build();
        else
            exchange.getResponse().getHeaders().add(CORRELATION_ID_KEY, corrId);
    }

    private static String setCorrelationId(ServerWebExchange exchange) {
        String corrId = UUID.randomUUID().toString();
        addHeader(exchange, corrId, true);
        return corrId;
    }

    @Bean
    RouteLocator routesConfig(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p.path("/isoft-bank/accounts/**")
                        .filters(f ->
                                f.rewritePath("/isoft-bank/accounts/(?<rest>.*)", "/${rest}")
                                        .addResponseHeader("X-ResponseTime", LocalDateTime.now().toString()))
                        .uri("lb://ACCOUNTS"))
                .route(p -> p.path("/isoft-bank/cards/**")
                        .filters(
                                f -> f.rewritePath("/isoft-bank/cards/(?<rest>.*)", "/${rest}")
                                        .addResponseHeader("X-ResponseTime", LocalDateTime.now().toString()))
                        .uri("lb://CARDS"))
                .route(p -> p.path("/isoft-bank/loans/**")
                        .filters(
                                f -> f.rewritePath("/isoft-bank/loans/(?<rest>.*)", "/${rest}")
                                        .addResponseHeader("X-ResponseTime", LocalDateTime.now().toString()))
                        .uri("lb://LOANS"))
                .build();
    }

    @Bean
    @Order(1)
    GlobalFilter requestTraceFilter() {
        return (exchange, chain) -> {
            processFilter(exchange, true);
            return chain.filter(exchange);
        };
    }

    @Bean
    @Order(1)
    GlobalFilter responseTraceFilter() {
        return (exchange, chain) -> chain.filter(exchange)
                .then(Mono.fromRunnable(() -> processFilter(exchange, false)));
    }

    private void processFilter(ServerWebExchange exchange, boolean isRequest) {
        String correlationIdValue, verb;
        List<String> requestCorrId = exchange.getRequest().getHeaders().get(CORRELATION_ID_KEY);

        if (nonNull(requestCorrId) && requestCorrId.size() != 0) {
            correlationIdValue = requestCorrId.get(0);
            if (!isRequest)
                addHeader(exchange, correlationIdValue, isRequest);
            verb = "found";
        } else {
            correlationIdValue = setCorrelationId(exchange);
            verb = "generated";
        }
        logger.info("correlation-id of " + (isRequest ? "request " : "response ") + verb + " : {}", correlationIdValue);
    }
}
