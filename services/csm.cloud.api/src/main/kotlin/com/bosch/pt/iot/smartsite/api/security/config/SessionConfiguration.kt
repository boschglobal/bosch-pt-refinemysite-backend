/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.config

import io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.session.data.mongo.AbstractMongoSessionConverter
import org.springframework.session.data.mongo.JacksonMongoSessionConverter
import org.springframework.web.server.session.CookieWebSessionIdResolver
import org.springframework.web.server.session.WebSessionIdResolver

@Configuration
class SessionConfiguration {

  @Bean
  fun webSessionIdResolver(environment: Environment): WebSessionIdResolver =
      CookieWebSessionIdResolver().apply {
        cookieName = COOKIE_NAME
        addCookieInitializer {
          if (environment.acceptsProfiles(Profiles.of("staging-open")))
              it.sameSite(SameSite.None.name)
          // Strict not possible due to redirects not sending cookie when SameSite set to strict
          else it.sameSite(SameSite.Lax.name)
        }
      }

  @Bean fun mongoSessionConverter(): AbstractMongoSessionConverter = JacksonMongoSessionConverter()

  companion object {
    const val COOKIE_NAME = "bosch-rms-auth-session"
  }
}
