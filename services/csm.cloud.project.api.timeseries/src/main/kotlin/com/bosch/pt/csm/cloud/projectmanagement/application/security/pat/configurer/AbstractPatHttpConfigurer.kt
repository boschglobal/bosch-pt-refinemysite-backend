/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.configurer

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.handler.PatAccessDeniedHandler
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationEntryPoint
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.TokenResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.core.env.Environment
import org.springframework.http.MediaType.ALL
import org.springframework.http.MediaType.APPLICATION_ATOM_XML
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.APPLICATION_XML
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_XML
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder
import org.springframework.security.config.annotation.web.HttpSecurityBuilder
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.access.DelegatingAccessDeniedHandler
import org.springframework.security.web.csrf.CsrfException
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.accept.ContentNegotiationStrategy
import org.springframework.web.accept.HeaderContentNegotiationStrategy

open class AbstractPatHttpConfigurer<T : AbstractHttpConfigurer<T, H>, H : HttpSecurityBuilder<H>>(
    private val context: ApplicationContext
) : AbstractHttpConfigurer<T, H>() {

  protected var authenticationManagerResolver: AuthenticationManagerResolver<HttpServletRequest>? =
      null

  protected lateinit var tokenResolver: TokenResolver

  protected var entryPoint: PatAuthenticationEntryPoint? = null
    get() =
        field
            ?: PatAuthenticationEntryPoint(
                context.getBean(MessageSource::class.java),
                context.getBean(Environment::class.java))

  protected val requestMatcher = PatRequestMatcher()

  private var accessDeniedHandler: AccessDeniedHandler =
      DelegatingAccessDeniedHandler(
          LinkedHashMap(createAccessDeniedHandlers()), PatAccessDeniedHandler()
      )

  private fun createAccessDeniedHandlers():
      HashMap<Class<out AccessDeniedException?>, AccessDeniedHandler> =
      HashMap<Class<out AccessDeniedException?>, AccessDeniedHandler>().apply {
        this[CsrfException::class.java] = AccessDeniedHandlerImpl()
      }

  fun accessDeniedHandler(
      accessDeniedHandler: AccessDeniedHandler
  ): AbstractPatHttpConfigurer<T, H> {
    this.accessDeniedHandler = accessDeniedHandler
    return this
  }

  fun authenticationManagerResolver(
      authenticationManagerResolver: AuthenticationManagerResolver<HttpServletRequest>
  ): AbstractPatHttpConfigurer<T, H> {
    this.authenticationManagerResolver = authenticationManagerResolver
    return this
  }

  fun tokenResolver(tokenResolver: TokenResolver): AbstractPatHttpConfigurer<T, H> {
    this.tokenResolver = tokenResolver
    return this
  }

  fun authenticationEntryPoint(
      entryPoint: PatAuthenticationEntryPoint
  ): AbstractPatHttpConfigurer<T, H> {
    this.entryPoint = entryPoint
    return this
  }

  override fun init(http: H) {
    registerDefaultAccessDeniedHandler(http)
    registerDefaultEntryPoint(http)
    registerDefaultCsrfOverride(http)
  }

  private fun registerDefaultAccessDeniedHandler(http: H) {
    http
        .getConfigurer(ExceptionHandlingConfigurer::class.java)
        ?.defaultAccessDeniedHandlerFor(this.accessDeniedHandler, this.requestMatcher)
  }

  private fun registerDefaultEntryPoint(http: H) {
    val exceptionHandling: ExceptionHandlingConfigurer<*>? =
        http.getConfigurer(ExceptionHandlingConfigurer::class.java)

    if (exceptionHandling != null) {
      var contentNegotiationStrategy = http.getSharedObject(ContentNegotiationStrategy::class.java)
      if (contentNegotiationStrategy == null) {
        contentNegotiationStrategy = HeaderContentNegotiationStrategy()
      }
      val restMatcher =
          MediaTypeRequestMatcher(
                  contentNegotiationStrategy,
                  APPLICATION_ATOM_XML,
                  APPLICATION_FORM_URLENCODED,
                  APPLICATION_JSON,
                  APPLICATION_OCTET_STREAM,
                  APPLICATION_XML,
                  MULTIPART_FORM_DATA,
                  TEXT_XML)
              .apply { setIgnoredMediaTypes(setOf(ALL)) }

      val allMatcher =
          MediaTypeRequestMatcher(contentNegotiationStrategy, ALL).apply { setUseEquals(true) }
      val notHtmlMatcher =
          NegatedRequestMatcher(MediaTypeRequestMatcher(contentNegotiationStrategy, TEXT_HTML))
      val restNotHtmlMatcher = AndRequestMatcher(listOf(notHtmlMatcher, restMatcher))
      val xRestedWithMatcher = RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")
      val preferredMatcher =
          OrRequestMatcher(
              listOf(requestMatcher, xRestedWithMatcher, restNotHtmlMatcher, allMatcher))

      exceptionHandling.defaultAuthenticationEntryPointFor(entryPoint, preferredMatcher)
    }
  }

  private fun registerDefaultCsrfOverride(http: H) {
    http.getConfigurer(CsrfConfigurer::class.java)?.ignoringRequestMatchers(requestMatcher)
  }

  class PatRequestMatcher : RequestMatcher {
    private var patResolver: TokenResolver? = null

    override fun matches(request: HttpServletRequest): Boolean =
        try {
          patResolver?.resolve(request) != null
        } catch (@Suppress("SwallowedException") e: OAuth2AuthenticationException) {
          false
        }

    fun setTokenResolver(patResolver: TokenResolver) {
      this.patResolver = patResolver
    }
  }

  fun postProcess() {
    super.postProcess(this)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <I> H.getConfigurer(configurerClass: Class<I>): I? {
    val clazz = Class.forName(AbstractConfiguredSecurityBuilder::class.java.name)

    return clazz
        .getDeclaredMethod("getConfigurer", Class::class.java)
        .apply { trySetAccessible() }
        .invoke(this@getConfigurer, configurerClass) as I?
  }
}
