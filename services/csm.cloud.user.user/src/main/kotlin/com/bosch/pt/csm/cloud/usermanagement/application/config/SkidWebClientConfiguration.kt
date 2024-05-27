/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Profile("skid-deleted-user-propagation")
@Configuration
class SkidWebClientConfiguration {

  @Bean
  fun skidWebClient(
      authorizedClientManager: OAuth2AuthorizedClientManager,
      @Autowired environment: Environment,
      @Value("\${custom.skid.baseUrl}") skidBaseUrl: String
  ): WebClient {
    val oauthFilter = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    return WebClient.builder()
        .codecs {
          // the "user deleted" endpoint needs ~82 bytes per deleted user returned.
          // as processing is done in batches of 100 users (optimizing for in-clause query) no more
          // than 8,2 KB should be needed (thus 1MB should be more than enough)
          it.defaultCodecs().maxInMemorySize(1 * 1024 * 1024)
        }
        .baseUrl(skidBaseUrl)
        .apply {
          // don't apply the oauth filter for tests
          if (environment.acceptsProfiles(Profiles.of("!test"))) {
            it.apply(oauthFilter.oauth2Configuration())
          }
        }
        .build()
  }
}
