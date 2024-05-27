/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.user.boundary

import com.bosch.pt.csm.cloud.event.user.integration.UserIntegrationService
import com.bosch.pt.csm.cloud.event.user.model.User
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class UserBoundaryService(private val userIntegrationService: UserIntegrationService) {

  fun findCurrentUser(): Mono<User> = userIntegrationService.findCurrentUser()
}
