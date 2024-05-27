/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKDAYCONFIGURATION
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.FRIDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.MONDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.THURSDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.TUESDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.WEDNESDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.CREATED
import java.time.Instant
import java.time.LocalDate.now
import java.util.UUID

@JvmOverloads
fun EventStreamGenerator.submitWorkdayConfiguration(
    asReference: String = "workdayConfiguration",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.identifier.toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: WorkdayConfigurationEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((WorkdayConfigurationAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  val existingWorkdayConfiguration = get<WorkdayConfigurationAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((WorkdayConfigurationAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((WorkdayConfigurationAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val workdayConfigurationEvent =
      existingWorkdayConfiguration.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          workdayConfigurationEvent.aggregate.aggregateIdentifier.buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          workdayConfigurationEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as WorkdayConfigurationEventAvro
  getContext().events[asReference] = sentEvent.aggregate

  return this
}

private fun WorkdayConfigurationAggregateAvro?.buildEventAvro(
    eventType: WorkdayConfigurationEventEnumAvro,
    vararg blocks: ((WorkdayConfigurationAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { WorkdayConfigurationEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newWorkdayConfiguration(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newWorkdayConfiguration(
    event: WorkdayConfigurationEventEnumAvro = CREATED
): WorkdayConfigurationEventAvro.Builder {
  val holidayAvro =
      HolidayAvro.newBuilder().setName("holiday").setDate(now().toEpochMilli()).build()

  val workdayConfiguration =
      WorkdayConfigurationAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(WORKDAYCONFIGURATION.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setStartOfWeek(MONDAY)
          .setWorkingDays(listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
          .setHolidays(listOf(holidayAvro))
          .setAllowWorkOnNonWorkingDays(true)

  return WorkdayConfigurationEventAvro.newBuilder()
      .setAggregateBuilder(workdayConfiguration)
      .setName(event)
}
