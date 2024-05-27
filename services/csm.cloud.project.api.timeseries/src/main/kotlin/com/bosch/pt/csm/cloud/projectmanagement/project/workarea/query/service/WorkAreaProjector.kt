/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkAreaMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkAreaVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.repository.WorkAreaRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class WorkAreaProjector(private val repository: WorkAreaRepository) {

  fun onWorkAreaEvent(aggregate: WorkAreaAggregateAvro, project: ProjectId) {
    val existingWorkArea = repository.findOneByIdentifier(aggregate.getIdentifier().asWorkAreaId())

    if (existingWorkArea == null ||
        aggregate.aggregateIdentifier.version > existingWorkArea.version) {
      (existingWorkArea?.updateFromWorkAreaAggregate(aggregate)
              ?: aggregate.toNewProjection(project))
          .apply { repository.save(this) }
    }
  }

  fun onWorkAreaDeletedEvent(aggregate: WorkAreaAggregateAvro) {
    val workArea = repository.findOneByIdentifier(aggregate.getIdentifier().asWorkAreaId())
    if (workArea != null && !workArea.deleted) {
      val newVersion =
          workArea.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.aggregateIdentifier.version,
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          WorkAreaMapper.INSTANCE.fromWorkAreaVersion(
              newVersion,
              workArea.identifier,
              workArea.project,
              workArea.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun WorkAreaAggregateAvro.toNewProjection(project: ProjectId): WorkArea {
    val workAreaVersion = this.newWorkAreaVersion()

    return WorkAreaMapper.INSTANCE.fromWorkAreaVersion(
        workAreaVersion = workAreaVersion,
        identifier = WorkAreaId(aggregateIdentifier.identifier.toUUID()),
        project = project,
        history = listOf(workAreaVersion))
  }

  private fun WorkArea.updateFromWorkAreaAggregate(aggregate: WorkAreaAggregateAvro): WorkArea {
    val workAreaVersion = aggregate.newWorkAreaVersion()

    return WorkAreaMapper.INSTANCE.fromWorkAreaVersion(
        workAreaVersion = workAreaVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(workAreaVersion) })
  }

  private fun WorkAreaAggregateAvro.newWorkAreaVersion(): WorkAreaVersion {
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

    return WorkAreaVersion(
        version = this.aggregateIdentifier.version,
        name = this.name,
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
