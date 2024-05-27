/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.UserReference

interface AuditingUser {
  fun getAuditUserId(): UserId
  fun getAuditUserVersion(): Long

  fun toUserReference() = UserReference(getAuditUserId(), getAuditUserVersion())
}
