/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * This controller was added as a compromise instead of using the actuator endpoint since it will be
 * available from the internet (to be used by the Azure Application Gateway) and we do not want to
 * provide information that we are using spring boot. This could be derived from the the path name.
 * Changing the path name from /actuator/health to something else was also not an option due to a
 * default prometheus configuration for all spring boot applications that we did not want to change
 * everywhere.
 */
@RestController
class HealthController {

  @GetMapping("/api/health", "/health") fun health(): Mono<String> = Mono.just("UP")
}
