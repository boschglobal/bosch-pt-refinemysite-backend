/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class TimelineRestController {

  /**
   * When using Basic Auth in Power BI, Power BI will validate the Basic credentials by invoking the
   * API root, i.e. /timeline. Only when this request succeeds, the validation succeeds. The body
   * content is irrelevant.
   *
   * This endpoint can also help clients other than Power BI to validate their Basic credentials
   * before invoking aggregate-specific endpoints.
   *
   * Although it is targeted at Basic Auth, the validation can also be used for other authentication
   * methods.
   */
  @GetMapping("", "/")
  fun isAuthenticated(): ResponseEntity<AuthenticationStatus> =
      ResponseEntity.ok().body(AuthenticationStatus(authenticated = true))

  // authenticated is always true because this code will only be reached by authenticated users
  data class AuthenticationStatus(val authenticated: Boolean)
}
