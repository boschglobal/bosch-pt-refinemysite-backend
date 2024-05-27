/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository

import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserDeletionState
import org.springframework.data.jpa.repository.JpaRepository

interface UserDeletionStateRepository : JpaRepository<UserDeletionState, Long>
