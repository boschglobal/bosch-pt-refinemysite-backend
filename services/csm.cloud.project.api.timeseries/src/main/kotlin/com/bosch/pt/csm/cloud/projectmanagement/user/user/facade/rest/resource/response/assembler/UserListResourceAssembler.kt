/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.UserListResource
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import org.springframework.stereotype.Component

@Component
class UserListResourceAssembler(private val userResourceAssembler: UserResourceAssembler) {

  fun assemble(users: List<UserProjection>): UserListResource =
      UserListResource(
          users
              .map { userResourceAssembler.assemble(it) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
