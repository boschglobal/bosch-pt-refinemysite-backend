/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.businesstransaction.event

import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportStartedEventAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder.currentBusinessTransactionId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime

class ProjectImportStartedEvent(
    projectIdentifier: ProjectId,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val createdBy: User = getCurrentUser()
) : AbstractProjectBusinessTransactionEvent(projectIdentifier) {

  override fun toAvroMessage(): ProjectImportStartedEventAvro =
      ProjectImportStartedEventAvro.newBuilder()
          .setAuditingInformation(buildEventAuditingInformation(createdDate, createdBy))
          .build()

  override fun toMessageKey() =
      BusinessTransactionStartedMessageKey(
          currentBusinessTransactionId()
              ?: error(
                  "Cannot create message key because no active business transaction was found."),
          projectIdentifier.toUuid())
}
