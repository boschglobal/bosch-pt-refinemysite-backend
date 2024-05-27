/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import java.util.Enumeration
import java.util.Locale
import java.util.Locale.GERMANY
import java.util.Locale.JAPAN
import java.util.Locale.US
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class CustomLocaleResolverTest {

  private val cut = CustomLocaleResolver()

  @BeforeEach
  fun init() {
    cut.setDefaultLocale(null)
    cut.supportedLocales = emptyList()
  }

  @Test
  fun verifyFailIfNoDefaultLocaleIsFound() {
    val request = Mockito.mock(MockHttpServletRequest::class.java)
    AssertionsForClassTypes.assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.resolveLocale(request) }
        .withMessage("No default locale has been found")
  }

  @Test
  fun verifyResolveLocaleFromRequest() {
    cut.setDefaultLocale(US)
    cut.supportedLocales = listOf(US, GERMANY)
    val locale = GERMANY
    val request = Mockito.mock(MockHttpServletRequest::class.java)
    Mockito.`when`(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn(locale.toString())
    Mockito.`when`(request.locales).thenReturn(asLocaleEnumerator(listOf(locale)))
    Assertions.assertThat(cut.resolveLocale(request)).isEqualTo(locale)
  }

  @Test
  fun verifyResolveLocaleFromRequestByLanguageOnly() {
    cut.setDefaultLocale(US)
    cut.supportedLocales = listOf(US, GERMANY)
    val locale = Locale.GERMAN
    val request = Mockito.mock(MockHttpServletRequest::class.java)
    Mockito.`when`(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)).thenReturn(locale.language)
    Mockito.`when`(request.locales).thenReturn(asLocaleEnumerator(listOf(locale)))
    Assertions.assertThat(cut.resolveLocale(request)).isEqualTo(GERMANY)
  }

  @Test
  fun verifyResolveLocaleFromPrincipal() {
    cut.setDefaultLocale(US)
    cut.supportedLocales = listOf(US, GERMANY)
    val locale = GERMANY
    val request = Mockito.mock(MockHttpServletRequest::class.java)
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(userLocale(locale), null)
    Assertions.assertThat(cut.resolveLocale(request)).isEqualTo(locale)
  }

  @Test
  fun verifyResolveLocaleGetDefaultLocale() {
    cut.setDefaultLocale(US)
    cut.supportedLocales = listOf(US, GERMANY)
    val locale = JAPAN
    val request = Mockito.mock(MockHttpServletRequest::class.java)
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(userLocale(locale), null)
    Assertions.assertThat(cut.resolveLocale(request)).isEqualTo(US)
  }

  @Test
  fun verifyGetSupportedLocale() {
    cut.supportedLocales = listOf(US, GERMANY)
    Assertions.assertThat(cut.getSupportedLocale(GERMANY)).isEqualTo(GERMANY)
  }

  @Test
  fun verifyGetSupportedLocaleFailsForUnsupportedLocale() {
    cut.supportedLocales = listOf(US, GERMANY)
    Assertions.assertThat(cut.getSupportedLocale(JAPAN)).isNull()
  }

  @Test
  fun verifyGetSupportedLocaleFailsForNoLocale() {
    Assertions.assertThat(cut.getSupportedLocale(null)).isNull()
  }

  @Test
  fun verifyGetUserLocale() {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(userLocale(GERMANY), null)
    Assertions.assertThat(cut.getUserLocale()).isEqualTo(GERMANY)
  }

  @Test
  fun verifyGetUserLocaleNullIfNotSet() {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(userLocale(null), null)
    Assertions.assertThat(cut.getUserLocale()).isNull()
  }

  @Test
  fun verifyGetUserLocaleFailsIfPrincipalIsNotUserLocale() {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(Any(), null)
    AssertionsForClassTypes.assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy(cut::getUserLocale)
        .withMessage("Principal doesn't implement required type UserLocale")
  }

  @Test
  fun verifyGetUserLocaleFailsIfNoPrincipal() {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(null, null)
    AssertionsForClassTypes.assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy(cut::getUserLocale)
        .withMessage("Could not find principal")
  }

  private fun userLocale(locale: Locale?): UserLocale {
    return object : UserLocale {
      override fun getUserLocale() = locale
    }
  }

  private fun asLocaleEnumerator(locales: Collection<Locale>): Enumeration<Locale> {
    return object : Enumeration<Locale> {
      private val localeIterator = locales.iterator()
      override fun hasMoreElements(): Boolean {
        return localeIterator.hasNext()
      }

      override fun nextElement(): Locale {
        return localeIterator.next()
      }
    }
  }
}
