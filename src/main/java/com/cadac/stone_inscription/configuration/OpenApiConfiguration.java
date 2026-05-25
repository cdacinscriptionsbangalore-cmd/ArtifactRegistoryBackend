package com.cadac.stone_inscription.configuration;

import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration
@SecurityScheme(
        name = OpenApiConfiguration.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
        // in = SecuritySchemeIn.HEADER
    )
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ApiErrorResponse";

    @Bean
    public OpenAPI stoneInscriptionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stone Inscription API")
                        .version("v1")
                        .description("""
                                REST API for OAuth authentication, user profiles, inscription posts, images, public dashboard metrics, and moderation reports.

                                Most JSON endpoints return a standard envelope with `message`, `http-status`, and `data`. Protected endpoints use a JWT bearer token in the `Authorization` header.
                                """)
                        .contact(new Contact().name("C-DAC Stone Inscription"))
                        .license(new License().name("Internal")))
                .components(new Components());
    }

    @Bean
    public GroupedOpenApi authenticationApi() {
        return GroupedOpenApi.builder()
                .group("01-authentication")
                .displayName("Authentication")
                .pathsToMatch("/oauth2/**", "/api/v1/**")
                .pathsToExclude("/api/v1/")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("02-users")
                .displayName("Users")
                .pathsToMatch("/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi postApi() {
        return GroupedOpenApi.builder()
                .group("03-posts")
                .displayName("Posts")
                .pathsToMatch("/post/**")
                .pathsToExclude("/post/test/**")
                .build();
    }

    @Bean
    public GroupedOpenApi reportApi() {
        return GroupedOpenApi.builder()
                .group("04-reports")
                .displayName("Reports")
                .pathsToMatch("/report", "/reports", "/moderate/**")
                .pathsToExclude("/test/**")
                .build();
    }

    @Bean
    public GlobalOperationCustomizer commonApiResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }

            if (isSecured(handlerMethod)) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                addErrorResponse(operation, "401", "JWT is missing, expired, or invalid.");
                addErrorResponse(operation, "403", "Authenticated user does not have the required role.");
            }

            addErrorResponse(operation, "400", "Request is syntactically valid but violates business rules.");
            addErrorResponse(operation, "422", "Bean validation failed for request body, form, or query parameters.");
            addErrorResponse(operation, "500", "Unexpected server error.");

            return operation;
        };
    }

    private boolean isSecured(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(Secured.class)
                || handlerMethod.getBeanType().isAnnotationPresent(Secured.class);
    }

    private void addErrorResponse(Operation operation, String code, String description) {
        if (operation.getResponses().containsKey(code)) {
            return;
        }

        operation.getResponses().addApiResponse(code, new ApiResponse()
                .description(description)
                .content(jsonContent(ERROR_SCHEMA_REF)));
    }

    private Content jsonContent(String schemaRef) {
        return new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(new Schema<>().$ref(schemaRef)));
    }
}
