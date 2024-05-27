/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.versioning

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SupportedApiVersionsController(private val supportedVersions: ApiVersionProperties) {

  @GetMapping("/api/v1/versions", "/v1/versions")
  fun getSupportedVersions(): Mono<ApiVersionProperties> = Mono.just(supportedVersions)
}
