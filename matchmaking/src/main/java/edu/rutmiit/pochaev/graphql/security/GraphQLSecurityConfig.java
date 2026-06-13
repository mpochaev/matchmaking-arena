package edu.rutmiit.pochaev.graphql.security;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Простая защита GraphQL и учебная трассировка запросов. */
@Configuration
public class GraphQLSecurityConfig {

    @Bean
    public Instrumentation maxQueryDepthInstrumentation() {
        return new MaxQueryDepthInstrumentation(20);
    }

    @Bean
    public Instrumentation maxQueryComplexityInstrumentation() {
        return new MaxQueryComplexityInstrumentation(200);
    }

    @Bean
    public Instrumentation tracingInstrumentation() {
        return new TracingInstrumentation();
    }
}
