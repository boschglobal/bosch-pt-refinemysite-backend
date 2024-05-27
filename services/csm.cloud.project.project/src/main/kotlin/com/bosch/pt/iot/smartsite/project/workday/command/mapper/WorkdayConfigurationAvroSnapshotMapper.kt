/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKDAYCONFIGURATION
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore.WorkdayConfigurationSnapshot
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import java.time.DayOfWeek

object WorkdayConfigurationAvroSnapshotMapper :
    AbstractAvroSnapshotMapper<WorkdayConfigurationSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: WorkdayConfigurationSnapshot,
      eventType: E
  ): WorkdayConfigurationEventAvro =
      WorkdayConfigurationEventAvro.newBuilder()
          .setName(eventType as WorkdayConfigurationEventEnumAvro)
          .setAggregateBuilder(
              WorkdayConfigurationAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                  .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                  .setProject(snapshot.projectRef.toAggregateReference())
                  .setStartOfWeek(DayEnumAvro.valueOf(snapshot.startOfWeek.name))
                  .setWorkingDays(snapshot.workingDays.toSortedDayEnumAvros())
                  .setHolidays(snapshot.holidays.toSortedHolidayAvros())
                  .setAllowWorkOnNonWorkingDays(snapshot.allowWorkOnNonWorkingDays))
          .build()

  override fun getAggregateType(): String = WORKDAYCONFIGURATION.value

  override fun getRootContextIdentifier(snapshot: WorkdayConfigurationSnapshot) =
      snapshot.projectRef.toUuid()

  private fun Set<DayOfWeek>.toSortedDayEnumAvros() = map { DayEnumAvro.valueOf(it.name) }.sorted()

  private fun Set<Holiday>.toSortedHolidayAvros() =
      map { HolidayAvro.newBuilder().setName(it.name).setDate(it.date.toEpochMilli()).build() }
          .sortedWith(compareBy({ it.date }, { it.name }))
}
