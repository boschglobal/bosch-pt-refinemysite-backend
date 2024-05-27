/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.asProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraft
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraftMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraftVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.repository.ProjectCraftRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class ProjectCraftProjector(private val repository: ProjectCraftRepository) {

  fun onProjectCraftEvent(aggregate: ProjectCraftAggregateG2Avro) {
    val existingCraft = repository.findOneByIdentifier(aggregate.getIdentifier().asProjectCraftId())

    if (existingCraft == null || aggregate.aggregateIdentifier.version > existingCraft.version) {
      (existingCraft?.updateFromProjectCraftAggregate(aggregate) ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  fun onProjectCraftDeletedEvent(aggregate: ProjectCraftAggregateG2Avro) {
    val craft = repository.findOneByIdentifier(aggregate.getIdentifier().asProjectCraftId())

    if (craft != null && !craft.deleted) {
      val newVersion =
          craft.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.aggregateIdentifier.version,
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          ProjectCraftMapper.INSTANCE.fromProjectCraftVersion(
              newVersion,
              identifier = craft.identifier,
              project = craft.project,
              history = craft.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun ProjectCraftAggregateG2Avro.toNewProjection(): ProjectCraft {
    val projectCraftVersion = this.newProjectCraftVersion()

    return ProjectCraftMapper.INSTANCE.fromProjectCraftVersion(
        projectCraftVersion = projectCraftVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asProjectCraftId(),
        project = project.identifier.toUUID().asProjectId(),
        history = listOf(projectCraftVersion))
  }

  private fun ProjectCraft.updateFromProjectCraftAggregate(
      aggregate: ProjectCraftAggregateG2Avro
  ): ProjectCraft {
    val projectCraftVersion = aggregate.newProjectCraftVersion()

    return ProjectCraftMapper.INSTANCE.fromProjectCraftVersion(
        projectCraftVersion = projectCraftVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(projectCraftVersion) })
  }

  private fun ProjectCraftAggregateG2Avro.newProjectCraftVersion(): ProjectCraftVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    return ProjectCraftVersion(
        version = this.aggregateIdentifier.version,
        name = this.name,
        color = this.color,
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
