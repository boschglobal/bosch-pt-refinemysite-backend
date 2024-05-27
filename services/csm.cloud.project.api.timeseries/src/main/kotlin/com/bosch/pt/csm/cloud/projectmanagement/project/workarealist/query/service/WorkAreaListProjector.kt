/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.domain.asWorkAreaListId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WorkAreaListMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WorkAreaListVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.repository.WorkAreaListRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class WorkAreaListProjector(private val repository: WorkAreaListRepository) {

  fun onWorkAreaListEvent(aggregate: WorkAreaListAggregateAvro) {
    val existingWorkAreaList =
        repository.findOneByIdentifier(
            aggregate.aggregateIdentifier.identifier.toUUID().asWorkAreaListId())

    if (existingWorkAreaList == null ||
        aggregate.aggregateIdentifier.version > existingWorkAreaList.version) {
      (existingWorkAreaList?.updateFromWorkAreaListAggregate(aggregate)
              ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  private fun WorkAreaListAggregateAvro.toNewProjection(): WorkAreaList {
    val workAreaListVersion = this.newWorkAreaListVersion()

    return WorkAreaListMapper.INSTANCE.fromWorkAreaListVersion(
        workAreaListVersion = workAreaListVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asWorkAreaListId(),
        project = project.identifier.toUUID().asProjectId(),
        history = listOf(workAreaListVersion))
  }

  private fun WorkAreaList.updateFromWorkAreaListAggregate(
      aggregate: WorkAreaListAggregateAvro
  ): WorkAreaList {
    val workAreaListVersion = aggregate.newWorkAreaListVersion()

    return WorkAreaListMapper.INSTANCE.fromWorkAreaListVersion(
        workAreaListVersion = workAreaListVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(workAreaListVersion) })
  }

  private fun WorkAreaListAggregateAvro.newWorkAreaListVersion(): WorkAreaListVersion {
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

    return WorkAreaListVersion(
        version = this.aggregateIdentifier.version,
        workAreas = this.workAreas.map { it.identifier.toUUID().asWorkAreaId() },
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
