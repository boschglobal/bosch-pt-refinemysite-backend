/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain.asWorkDayConfigurationId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.DayEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.Holiday
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfigurationMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfigurationVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.repository.WorkDayConfigurationRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.workday.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class WorkDayConfigurationProjector(private val repository: WorkDayConfigurationRepository) {

  fun onWorkDayConfigurationEvent(aggregate: WorkdayConfigurationAggregateAvro) {
    val existingWorkDayConfiguration =
        repository.findOneByIdentifier(aggregate.getIdentifier().asWorkDayConfigurationId())

    if (existingWorkDayConfiguration == null ||
        aggregate.aggregateIdentifier.version > existingWorkDayConfiguration.version) {

      (existingWorkDayConfiguration?.updateFromWorkDayConfigurationAggregate(aggregate)
              ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  fun onWorkDayConfigurationDeletedEvent(aggregate: WorkdayConfigurationAggregateAvro) {
    val workDayConfiguration =
        repository.findOneByIdentifier(aggregate.getIdentifier().asWorkDayConfigurationId())

    if (workDayConfiguration != null && !workDayConfiguration.deleted) {
      val newVersion =
          workDayConfiguration.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.aggregateIdentifier.version,
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          WorkDayConfigurationMapper.INSTANCE.fromWorkDayConfigurationVersion(
              newVersion,
              workDayConfiguration.identifier,
              workDayConfiguration.project,
              workDayConfiguration.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun WorkdayConfigurationAggregateAvro.toNewProjection(): WorkDayConfiguration {
    val workDayConfigurationVersion = this.newWorkDayConfigurationVersion()

    return WorkDayConfigurationMapper.INSTANCE.fromWorkDayConfigurationVersion(
        workDayConfigurationVersion = workDayConfigurationVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asWorkDayConfigurationId(),
        project = this.project.identifier.toUUID().asProjectId(),
        history = listOf(workDayConfigurationVersion))
  }

  private fun WorkDayConfiguration.updateFromWorkDayConfigurationAggregate(
      aggregate: WorkdayConfigurationAggregateAvro
  ): WorkDayConfiguration {
    val workDayConfigurationVersion = aggregate.newWorkDayConfigurationVersion()

    return WorkDayConfigurationMapper.INSTANCE.fromWorkDayConfigurationVersion(
        workDayConfigurationVersion = workDayConfigurationVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(workDayConfigurationVersion) })
  }

  private fun WorkdayConfigurationAggregateAvro.newWorkDayConfigurationVersion():
      WorkDayConfigurationVersion {
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

    return WorkDayConfigurationVersion(
        version = this.aggregateIdentifier.version,
        startOfWeek = DayEnum.valueOf(this.startOfWeek.name),
        workingDays = this.workingDays.map { DayEnum.valueOf(it.name) },
        holidays = this.holidays.map { Holiday(it.name, it.date.toLocalDateByMillis()) },
        allowWorkOnNonWorkingDays = this.allowWorkOnNonWorkingDays,
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
