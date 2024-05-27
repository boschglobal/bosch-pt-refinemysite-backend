/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.businesstransaction.event

import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.reschedule.messages.ProjectRescheduleStartedEventAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder.currentBusinessTransactionId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime

class ProjectRescheduleStartedEvent(
    projectIdentifier: ProjectId,
    val shiftDays: Long,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val createdBy: User = getCurrentUser()
) : AbstractProjectBusinessTransactionEvent(projectIdentifier) {

  override fun toAvroMessage(): ProjectRescheduleStartedEventAvro =
      ProjectRescheduleStartedEventAvro.newBuilder()
          .setAuditingInformation(buildEventAuditingInformation(createdDate, createdBy))
          .setShiftDays(shiftDays.toInt())
          .build()

  override fun toMessageKey() =
      BusinessTransactionStartedMessageKey(
          currentBusinessTransactionId()
              ?: error(
                  "Cannot create message key because no active business transaction was found."),
          projectIdentifier.toUuid())
}
