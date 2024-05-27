/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.repository

import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID

interface UserRepositoryExtension {

  fun findWithDetailsByIdentifier(identifier: UUID): User?
}
