/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.util

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKDAYCONFIGURATION
import com.bosch.pt.csm.cloud.projectmanagement.workday.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2.Companion.validateAuditingInformationAndIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2.Companion.validateCreatedAggregateAuditInfoAndAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2.Companion.validateUpdatedAggregateAuditInfoAndAggregateIdentifier
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import org.assertj.core.api.Assertions.assertThat

object WorkdayConfigurationTestUtils {

  fun verifyCreatedAggregate(
      aggregate: WorkdayConfigurationAggregateAvro,
      projectIdentifier: ProjectId,
      testUser: User
  ) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(
            this, WORKDAYCONFIGURATION, testUser)
        assertThat(getProjectIdentifier()).isEqualTo(projectIdentifier.identifier)
        assertThat(startOfWeek.name).isEqualTo(MONDAY.name)
        assertThat(workingDays.map { it.name })
            .containsExactly(MONDAY.name, TUESDAY.name, WEDNESDAY.name, THURSDAY.name, FRIDAY.name)
        assertThat(holidays).isEmpty()
        assertThat(allowWorkOnNonWorkingDays).isTrue
      }

  fun verifyUpdatedAggregate(
      aggregate: WorkdayConfigurationAggregateAvro,
      workdayConfiguration: WorkdayConfiguration
  ) =
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, workdayConfiguration, WORKDAYCONFIGURATION)
        assertThat(getProjectIdentifier())
            .isEqualTo(workdayConfiguration.project.identifier.identifier)
        assertThat(startOfWeek.name).isEqualTo(workdayConfiguration.startOfWeek.name)
        assertThat(workingDays)
            .containsExactlyElementsOf(workdayConfiguration.workingDays.toSortedDayEnumAvros())
        assertThat(holidays)
            .containsExactlyElementsOf(workdayConfiguration.holidays.toSortedHolidayAvros())
        assertThat(allowWorkOnNonWorkingDays)
            .isEqualTo(workdayConfiguration.allowWorkOnNonWorkingDays)
      }

  fun validateWorkdayConfiguration(
      workdayConfiguration: WorkdayConfiguration,
      aggregate: WorkdayConfigurationAggregateAvro,
      projectIdentifier: ProjectId
  ) =
      with(workdayConfiguration) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(startOfWeek.name).isEqualTo(aggregate.startOfWeek.name)
        assertThat(workingDays)
            .containsExactlyInAnyOrderElementsOf(
                aggregate.workingDays.map { DayOfWeek.valueOf(it.name) })
        assertThat(holidays)
            .containsExactlyInAnyOrderElementsOf(
                aggregate.holidays.map { Holiday(it.name, it.date.toLocalDateByMillis()) })
        assertThat(allowWorkOnNonWorkingDays).isEqualTo(aggregate.allowWorkOnNonWorkingDays)
      }

  private fun Set<DayOfWeek>.toSortedDayEnumAvros() = map { DayEnumAvro.valueOf(it.name) }.sorted()

  private fun Set<Holiday>.toSortedHolidayAvros() =
      map { HolidayAvro.newBuilder().setName(it.name).setDate(it.date.toEpochMilli()).build() }
          .sortedWith(compareBy({ it.date }, { it.name }))
}
