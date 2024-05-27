/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import java.time.LocalDateTime

interface AuditableSnapshot {
  val createdDate: LocalDateTime?
  val lastModifiedDate: LocalDateTime?
  val createdBy: UserId?
  val lastModifiedBy: UserId?
}
