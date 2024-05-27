/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.UserPhoneNumberDto
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.UserResource
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import org.springframework.stereotype.Component

@Component
class UserResourceAssembler {

  fun assemble(user: UserProjection): UserResource =
      user.history.last().let {
        UserResourceMapper.INSTANCE.fromUserVersion(
            it,
            user.identifier,
            it.phoneNumbers.map {
              UserPhoneNumberDto(it.countryCode, it.phoneNumberType.key, it.callNumber)
            })
      }
}
