/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.asProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.asMilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.MilestoneMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.MilestoneTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.MilestoneVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.repository.MilestoneRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class MilestoneProjector(private val repository: MilestoneRepository) {

  fun onMilestoneEvent(aggregate: MilestoneAggregateAvro) {
    val existingMilestone =
        repository.findOneByIdentifier(aggregate.getIdentifier().asMilestoneId())

    if (existingMilestone == null || aggregate.getVersion() > existingMilestone.version) {
      (existingMilestone?.updateFromMilestoneAggregate(aggregate) ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  fun onMilestoneDeletedEvent(aggregate: MilestoneAggregateAvro) {
    val milestone = repository.findOneByIdentifier(aggregate.getIdentifier().asMilestoneId())
    if (milestone != null && !milestone.deleted) {
      val newVersion =
          milestone.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.getVersion(),
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          MilestoneMapper.INSTANCE.fromMilestoneVersion(
              newVersion,
              milestone.identifier,
              milestone.project,
              milestone.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun MilestoneAggregateAvro.toNewProjection(): Milestone {
    val milestoneVersion = this.newTaskVersion()

    return MilestoneMapper.INSTANCE.fromMilestoneVersion(
        milestoneVersion = milestoneVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asMilestoneId(),
        project = project.identifier.toUUID().asProjectId(),
        history = listOf(milestoneVersion))
  }

  private fun Milestone.updateFromMilestoneAggregate(aggregate: MilestoneAggregateAvro): Milestone {
    val milestoneVersion = aggregate.newTaskVersion()

    return MilestoneMapper.INSTANCE.fromMilestoneVersion(
        milestoneVersion = milestoneVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(milestoneVersion) })
  }

  private fun MilestoneAggregateAvro.newTaskVersion(): MilestoneVersion {
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

    return MilestoneVersion(
        version = this.aggregateIdentifier.version,
        name = this.name,
        type = MilestoneTypeEnum.valueOf(this.type.name),
        date = this.date.toLocalDateByMillis(),
        header = this.header,
        description = this.description,
        craft = this.craft?.identifier?.toUUID()?.asProjectCraftId(),
        workArea = this.workarea?.identifier?.toUUID()?.asWorkAreaId(),
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
