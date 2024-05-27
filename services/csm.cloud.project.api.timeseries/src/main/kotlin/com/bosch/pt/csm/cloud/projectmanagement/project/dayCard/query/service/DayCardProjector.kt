/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getDayCardVersion
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.asDayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class DayCardProjector(private val repository: DayCardRepository) {

  fun onDayCardEvent(aggregate: DayCardAggregateG2Avro, project: ProjectId) {
    val existingDayCard = repository.findOneByIdentifier(aggregate.getIdentifier().asDayCardId())

    if (existingDayCard == null || aggregate.getDayCardVersion() > existingDayCard.version) {
      (existingDayCard?.updateFromDayCardAggregate(aggregate) ?: aggregate.toNewProjection(project))
          .apply { repository.save(this) }
    }
  }

  /*
   * Mark day cards only as deleted. Day cards are deleted with their schedules.
   * */
  fun onDayCardDeletedEvent(aggregate: DayCardAggregateG2Avro) {
    val dayCard = repository.findOneByIdentifier(aggregate.getIdentifier().asDayCardId())
    if (dayCard != null && !dayCard.deleted) {
      val newVersion =
          dayCard.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.aggregateIdentifier.version,
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          DayCardMapper.INSTANCE.fromDayCardVersion(
              dayCardVersion = newVersion,
              identifier = dayCard.identifier,
              project = dayCard.project,
              task = dayCard.task,
              history = dayCard.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun DayCardAggregateG2Avro.toNewProjection(project: ProjectId): DayCard {
    val dayCardVersion = this.newDayCardVersion()

    return DayCardMapper.INSTANCE.fromDayCardVersion(
        dayCardVersion = dayCardVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asDayCardId(),
        project = project,
        task = task.identifier.toUUID().asTaskId(),
        history = listOf(dayCardVersion))
  }

  private fun DayCard.updateFromDayCardAggregate(aggregate: DayCardAggregateG2Avro): DayCard {
    val dayCardVersion = aggregate.newDayCardVersion()

    return DayCardMapper.INSTANCE.fromDayCardVersion(
        dayCardVersion = dayCardVersion,
        identifier = this.identifier,
        project = this.project,
        task = this.task,
        history = this.history.toMutableList().also { it.add(dayCardVersion) })
  }

  private fun DayCardAggregateG2Avro.newDayCardVersion(): DayCardVersion {
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

    return DayCardVersion(
        version = this.aggregateIdentifier.version,
        status = DayCardStatusEnum.valueOf(this.status.name),
        title = this.title,
        manpower = this.manpower,
        notes = this.notes,
        reason = this.reason?.let { DayCardReasonEnum.valueOf(it.name) },
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
