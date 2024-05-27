/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.repository

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.model.ConsentsUser
import org.springframework.data.jpa.repository.JpaRepository

interface ConsentsUserRepository : JpaRepository<ConsentsUser, Long> {
  fun findByIdentifier(identifier: UserId): ConsentsUser?
}
