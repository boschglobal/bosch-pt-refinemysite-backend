/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.configurer

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationFilter
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.PatResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authorization.PatScopeAuthorizationFilter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.ApplicationContext
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.config.annotation.web.HttpSecurityBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.access.ExceptionTranslationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository

class PatHttpConfigurer<H : HttpSecurityBuilder<H>>(context: ApplicationContext) :
    AbstractPatHttpConfigurer<BasicPatHttpConfigurer<H>, H>(context) {

  init {
    val patResolver =
        if (context.getBeanNamesForType(PatResolver::class.java).isNotEmpty()) {
          context.getBean(PatResolver::class.java)
        } else {
          PatResolver()
        }

    tokenResolver(patResolver)
  }

  override fun configure(http: H) {
    requestMatcher.setTokenResolver(tokenResolver)

    val resolver: AuthenticationManagerResolver<HttpServletRequest> =
        authenticationManagerResolver
            ?: AuthenticationManagerResolver<HttpServletRequest> {
              http.getSharedObject(AuthenticationManager::class.java)
            }

    val filter =
        PatAuthenticationFilter(
                resolver,
                getSecurityContextHolderStrategy(),
                tokenResolver,
                requireNotNull(entryPoint),
                RequestAttributeSecurityContextRepository(),
                WebAuthenticationDetailsSource())
            .also { postProcess(it) }

    http.addFilterBefore(filter, BearerTokenAuthenticationFilter::class.java)

    // Register filter for pat scope based authorization
    val authorizationFilter = PatScopeAuthorizationFilter()
    http.addFilterAfter(authorizationFilter, ExceptionTranslationFilter::class.java)
  }
}

fun HttpSecurity.patAuthentication(): PatHttpConfigurer<out HttpSecurityBuilder<*>> {
  val applicationContext = this.getSharedObject(ApplicationContext::class.java)
  return this.apply(PatHttpConfigurer(applicationContext)).also { it.postProcess() }
}
