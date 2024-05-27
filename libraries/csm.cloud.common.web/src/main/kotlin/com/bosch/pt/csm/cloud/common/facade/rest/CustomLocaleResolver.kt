/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import jakarta.servlet.http.HttpServletRequest
import java.util.Collections
import java.util.Locale
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver

class CustomLocaleResolver : AcceptHeaderLocaleResolver() {

  override fun resolveLocale(request: HttpServletRequest): Locale {
    val defaultLocale =
        defaultLocale ?: throw IllegalArgumentException("No default locale has been found")
    val requestLocales = ArrayList<Locale>()
    getRequestLocale(request).forEachRemaining { e: Locale -> requestLocales.add(e) }
    var locale = getSupportedLocale(requestLocales)
    if (locale != null) {
      return locale
    }

    // Fallback in case a client just sends the language instead of a locale
    locale = getSupportedLocaleByLanguage(requestLocales)
    if (locale != null) {
      return locale
    }
    locale = getSupportedLocale(getUserLocale())
    return locale ?: defaultLocale
  }

  fun getSupportedLocale(locale: Locale?): Locale? {
    return if (locale != null) {
      getSupportedLocale(java.util.List.of(locale))
    } else null
  }

  private fun getSupportedLocale(locales: List<Locale>): Locale? {
    val supportedLocales = supportedLocales
    for (locale in locales) {
      if (supportedLocales.contains(locale)) {
        return locale
      }
    }
    return null
  }

  fun getSupportedLocaleByLanguage(locales: List<Locale>): Locale? {
    val supportedLocales = supportedLocales
    for (locale in locales) {
      val language =
          supportedLocales
              .stream()
              .filter { supportedLocale: Locale ->
                if (locale.language != null) {
                  return@filter supportedLocale.language.equals(locale.language, ignoreCase = true)
                }
                false
              }
              .findFirst()
      if (language.isPresent) {
        return language.get()
      }
    }
    return null
  }

  fun getUserLocale(): Locale? {
    val userPrincipal = getUserPrincipal()
    if (userPrincipal is UserLocale) {
      val userLocale = userPrincipal
      return if (userLocale.getUserLocale() == null) {
        null
      } else userLocale.getUserLocale()
    }
    error("Principal doesn't implement required type UserLocale")
  }

  private fun getRequestLocale(request: HttpServletRequest): Iterator<Locale> {
    return if (request.getHeader(HttpHeaders.ACCEPT_LANGUAGE) == null) {
      Collections.emptyIterator()
    } else request.locales.asIterator()
  }

  @ExcludeFromCodeCoverageGenerated
  private fun getUserPrincipal(): Any =
      if (SecurityContextHolder.getContext() != null &&
          SecurityContextHolder.getContext().authentication != null &&
          SecurityContextHolder.getContext().authentication.principal != null) {
        SecurityContextHolder.getContext().authentication.principal
      } else {
        error("Could not find principal")
      }
}
