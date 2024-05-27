/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2
import io.swagger.v3.oas.models.servers.Server
import java.util.Collections
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(info = Info(title = "Project API"))
@ConfigurationProperties(prefix = "custom.swagger")
open class OpenApiConfiguration(var url: String = "/internal") {

  @Bean
  open fun customOpenAPI(): OpenAPI {
    return OpenAPI()
        .addServersItem(Server().url(url))
        .components(
            Components()
                .addSecuritySchemes(
                    "oauth2",
                    SecurityScheme()
                        .type(OAUTH2)
                        .flows(
                            OAuthFlows()
                                .authorizationCode(
                                    OAuthFlow()
                                        .authorizationUrl(
                                            "https://p32.authz.bosch.com/auth/realms/central_profile/" +
                                                "protocol/openid-connect/auth")
                                        .tokenUrl(
                                            "https://p32.authz.bosch.com/auth/realms/central_profile/" +
                                                "protocol/openid-connect/token")
                                        .scopes(
                                            Scopes()
                                                .addString("openid", "openid")
                                                .addString("profile", "profile")
                                                .addString("offline_access", "offline_access"))))))
        .security(Collections.singletonList(SecurityRequirement().addList("oauth2")))
  }
}
