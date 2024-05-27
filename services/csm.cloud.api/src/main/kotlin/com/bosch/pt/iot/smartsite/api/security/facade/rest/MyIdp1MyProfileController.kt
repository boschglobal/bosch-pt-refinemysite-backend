/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.facade.rest

import com.bosch.pt.iot.smartsite.api.security.RedirectConstants.REDIRECT_URL_PARAMETER
import com.bosch.pt.iot.smartsite.api.security.config.KeyCloak1Configuration.Companion.KEYCLOAK1
import com.bosch.pt.iot.smartsite.api.security.config.MyIdp1ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just

@Profile(KEYCLOAK1)
@RestController
class MyIdp1MyProfileController(
    private val myidp1ConfigurationProperties: MyIdp1ConfigurationProperties
) {

  /**
   * API method as well as parameter [redirectUri] are kept here for backward compatibility only.
   * Please refer to /profile endpoint instead.
   */
  @Suppress("UnusedPrivateMember")
  @Deprecated(
      message = "MyIdp1 does not support direct entry to change-password.",
      replaceWith = ReplaceWith("redirectToMyIdp1MyProfile()"),
  )
  @GetMapping("/api/change-password", "/change-password")
  fun resetPassword(
      @RequestParam(name = REDIRECT_URL_PARAMETER, required = false) redirectUri: String
  ): Mono<ResponseEntity<Any>> = redirectToMyIdp1MyProfile()

  /** Redirects to MYIDP1 MyProfile page (change email, password, delete SingleKeyID). */
  @GetMapping("/api/profile", "/profile")
  fun redirectToMyIdp1MyProfile(): Mono<ResponseEntity<Any>> =
      just(status(FOUND).location(myidp1ConfigurationProperties.userProfileUrl).build())
}
