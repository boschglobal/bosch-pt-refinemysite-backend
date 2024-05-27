/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import java.net.URI
import java.util.Base64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedirectUrlValidationServiceTest {

  private lateinit var cut: RedirectUrlValidationService

  @BeforeEach
  fun setup() {
    val whitelist = RedirectWhitelist(listOf(URI("https://localhost:8000")))
    cut = RedirectUrlValidationService(whitelist)
  }

  /**
   * to understand this test, learn about the difference between "base64" and "base64url". They are
   * not the same and are not compatible with each other.
   *
   * @see https://www.rfc-editor.org/rfc/rfc4648.html#section-5
   */
  @Test
  fun `decode redirect url that has different encodings for base64 and base64url`() {
    // for many strings, the base64 encoding is the same as the base64url encoding. In this test, we
    // need to carefully choose a redirectUrl for which these encodings are different.
    val redirectUrl =
        "https://localhost:8000/projects/d6309540-2207-4169-a8a7-c4f57b86e090/calendar?mode=sixweeks&start=2023-01-09"
    assertThat(redirectUrl.toBase64Url()).isNotEqualTo(redirectUrl.toBase64())

    val encoded = redirectUrl.toBase64Url()
    val decoded = cut.decodeRedirectString(encoded)

    assertThat(decoded).isEqualTo(URI(redirectUrl))
  }

  @Test
  fun `redirect is allowed for whitelisted url`() {
    val redirectAllowed = cut.isRedirectAllowed(URI("https://localhost:8000"))

    assertThat(redirectAllowed).isTrue
  }

  @Test
  fun `redirect is allowed for whitelisted url with path`() {
    val redirectAllowed = cut.isRedirectAllowed(URI("https://localhost:8000/path/"))

    assertThat(redirectAllowed).isTrue
  }

  @Test
  fun `redirect is allowed for whitelisted url with query string`() {
    val redirectAllowed = cut.isRedirectAllowed(URI("https://localhost:8000/?key=val"))

    assertThat(redirectAllowed).isTrue
  }

  @Test
  fun `redirect is allowed for whitelisted url with path and query string`() {
    val redirectAllowed = cut.isRedirectAllowed(URI("https://localhost:8000/path?key=val"))

    assertThat(redirectAllowed).isTrue
  }

  @Test
  fun `redirect is denied for a non-whitelisted url`() {
    val redirectAllowed = cut.isRedirectAllowed(URI("https://not.whitelisted"))

    assertThat(redirectAllowed).isFalse
  }

  private fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray())

  private fun String.toBase64Url() = Base64.getUrlEncoder().encodeToString(this.toByteArray())
}
