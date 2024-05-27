/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.query.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import org.springframework.data.domain.Auditable

data class ResolvedUserNamesDto(val userNames: Map<UserId, String>) {

  fun <T : UuidIdentifiable> createdByOf(auditable: Auditable<T, *, *>): String =
      userNames[UserId(auditable.createdBy.get().toUuid())] ?: ""

  fun <T : UuidIdentifiable> lastModifiedByOf(auditable: Auditable<T, *, *>): String =
      userNames[UserId(auditable.lastModifiedBy.get().toUuid())] ?: ""
}
