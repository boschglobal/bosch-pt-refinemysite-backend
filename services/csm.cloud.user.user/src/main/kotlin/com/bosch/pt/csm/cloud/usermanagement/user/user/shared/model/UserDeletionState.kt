/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model

import com.bosch.pt.csm.cloud.common.api.LocalEntity
import jakarta.persistence.Entity
import java.time.LocalDateTime

@Entity class UserDeletionState(var deletedToDateTime: LocalDateTime) : LocalEntity<Long>()
