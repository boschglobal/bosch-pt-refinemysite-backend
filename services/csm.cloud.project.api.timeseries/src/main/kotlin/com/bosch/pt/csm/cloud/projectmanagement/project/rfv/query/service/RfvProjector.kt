/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.domain.asRfvId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.Rfv
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.RfvMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.RfvVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.repository.RfvRepository
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class RfvProjector(private val repository: RfvRepository) {

  fun onRfvEvent(aggregate: RfvCustomizationAggregateAvro) {
    val existingRfv = repository.findOneByIdentifier(aggregate.getIdentifier().asRfvId())

    if (existingRfv == null || aggregate.aggregateIdentifier.version > existingRfv.version) {
      (existingRfv?.updateFromRfvAggregate(aggregate) ?: aggregate.toNewProjection()).apply {
        repository.save(this)
      }
    }
  }

  fun onRfvDeletedEvent(aggregate: RfvCustomizationAggregateAvro) {
    val rfv = repository.findOneByIdentifier(aggregate.getIdentifier().asRfvId())
    if (rfv != null && !rfv.deleted) {
      val newVersion =
          rfv.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.getVersion(),
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          RfvMapper.INSTANCE.fromRfvVersion(
              newVersion,
              rfv.identifier,
              rfv.project,
              rfv.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun RfvCustomizationAggregateAvro.toNewProjection(): Rfv {
    val rfvVersion = this.newRfvVersion()

    return RfvMapper.INSTANCE.fromRfvVersion(
        rfvVersion = rfvVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asRfvId(),
        project = project.identifier.toUUID().asProjectId(),
        history = listOf(rfvVersion))
  }

  private fun Rfv.updateFromRfvAggregate(aggregate: RfvCustomizationAggregateAvro): Rfv {
    val rfvVersion = aggregate.newRfvVersion()

    return RfvMapper.INSTANCE.fromRfvVersion(
        rfvVersion = rfvVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(rfvVersion) })
  }

  private fun RfvCustomizationAggregateAvro.newRfvVersion(): RfvVersion {
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

    return RfvVersion(
        version = this.aggregateIdentifier.version,
        reason = DayCardReasonEnum.valueOf(this.key.name),
        active = this.active,
        name = this.name,
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
