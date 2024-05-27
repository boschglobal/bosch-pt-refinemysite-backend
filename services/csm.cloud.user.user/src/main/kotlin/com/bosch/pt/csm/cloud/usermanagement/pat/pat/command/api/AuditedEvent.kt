/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.api.UserId
import java.time.LocalDateTime

@LibraryCandidate("this could be used universally")
interface AuditedEvent {
  val userIdentifier: UserId
  val timestamp: LocalDateTime
}
