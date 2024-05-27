/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.UserPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.UserPhoneNumberPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import org.springframework.stereotype.Component

@Component
class UserPayloadAssembler {

  fun assemble(userProjection: UserProjection): UserPayloadV1 =
      UserPayloadMapper.INSTANCE.fromUserProjection(
          userProjection,
          userProjection.phoneNumbers.map {
            UserPhoneNumberPayloadV1(it.countryCode, it.phoneNumberType.shortKey, it.callNumber)
          })
}
