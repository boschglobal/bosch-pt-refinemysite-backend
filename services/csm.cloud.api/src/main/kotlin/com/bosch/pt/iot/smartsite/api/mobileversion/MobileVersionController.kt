/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.mobileversion

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class MobileVersionController(private val lastSupportedVersion: MobileVersionProperty) {

  @GetMapping("/api/mobile/version", "/internal/mobile/version", "/mobile/version")
  fun getLastSupportedVersion(): Mono<MobileVersionProperty> = Mono.just(lastSupportedVersion)
}
