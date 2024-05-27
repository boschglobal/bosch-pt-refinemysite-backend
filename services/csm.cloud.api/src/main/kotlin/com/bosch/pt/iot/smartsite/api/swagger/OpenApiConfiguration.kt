/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.swagger

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition
class OpenApiConfiguration {

  @Value("\${custom.security.oauth2.resource-server.jwt.issuer-uris[0]}")
  private lateinit var idpJwtIssuerUri: String

  @Bean
  fun customOpenAPI(): OpenAPI {
    return OpenAPI()
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
                                            "$idpJwtIssuerUri/protocol/openid-connect/auth")
                                        .tokenUrl("$idpJwtIssuerUri/protocol/openid-connect/token")
                                        .scopes(
                                            Scopes()
                                                .addString("openid", "openid")
                                                .addString("profile", "profile")
                                                .addString("offline_access", "offline_access"))))))
        .security(listOf(SecurityRequirement().addList("oauth2")))
  }
}
