/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.boundary.AsynchronousDeleteServiceV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_DELETE_VALIDATION_ERROR_PROJECT_TITLE_INCORRECT
import com.bosch.pt.iot.smartsite.common.kafka.AggregateIdentifierUtils.getAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ProjectDeleteService(
    private val projectRepository: ProjectRepository,
    private val commandSendingService: CommandSendingService,
    private val logger: Logger
) : AsynchronousDeleteServiceV2<ProjectId> {

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasDeletePermissionOnProject(#identifier)")
  @Transactional
  override fun markAsDeletedAndSendEvent(identifier: ProjectId) {
    val project =
        projectRepository.findOneByIdentifier(identifier)
            ?: throw AccessDeniedException("User has no access to this project")

    // Create message to send
    val key = CommandMessageKey(project.identifier.toUuid())
    val deleteCommandAvro =
        DeleteCommandAvro(
            getAggregateIdentifier(project, PROJECT.value),
            getAggregateIdentifier(
                SecurityContextHelper.getInstance().getCurrentUser(), USER.value))

    // Mark the project as deleted without sending an event and increasing the hibernate version
    markAsDeleted(project.identifier)

    // Send message to delete the project
    commandSendingService.send(key, deleteCommandAvro, "project-delete")
    logger.info("Project $identifier was marked to be deleted")
  }

  @Trace
  @NoPreAuthorize(usedByController = true)
  @Transactional(readOnly = true)
  open fun validateProjectDeletion(targetIdentifier: ProjectId, verificationTitle: String) {
    val project = projectRepository.findOneByIdentifier(targetIdentifier)!!
    if (project.title != verificationTitle) {
      throw PreconditionViolationException(PROJECT_DELETE_VALIDATION_ERROR_PROJECT_TITLE_INCORRECT)
    }
  }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  override fun markAsDeleted(identifier: ProjectId) {
    projectRepository.findOneByIdentifier(identifier)?.apply {
      projectRepository.markAsDeleted(this.id!!)
    }
  }
}
