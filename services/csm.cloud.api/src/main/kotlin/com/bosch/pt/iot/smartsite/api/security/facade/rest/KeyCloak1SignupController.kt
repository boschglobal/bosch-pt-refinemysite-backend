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
import java.net.URI.create
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
class KeyCloak1SignupController {

  /** Redirects to /login as it is the exact same flow with KEYCLOAK1 (and SingleKeyID). */
  @GetMapping("/signup", "/api/signup")
  fun signupInternal(
      @RequestParam(name = REDIRECT_URL_PARAMETER) redirectUri: String
  ): Mono<ResponseEntity<Any>> =
      just(status(FOUND).location(create("/login?$REDIRECT_URL_PARAMETER=$redirectUri")).build())
}
