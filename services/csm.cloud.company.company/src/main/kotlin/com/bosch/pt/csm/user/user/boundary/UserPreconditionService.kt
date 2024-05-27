/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.boundary

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.common.api.UserId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class UserPreconditionService(
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId
) {

  @AdminAuthorization
  @Transactional(propagation = Propagation.SUPPORTS)
  fun isDeleteUserPossible(identifier: UserId?): Boolean =
      identifier != null && systemUserIdentifier != identifier
}
