/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore.WorkdayConfigurationSnapshot
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WorkdayConfigurationAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createWorkConfigurationSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = WorkdayConfigurationAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createWorkConfigurationSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      WorkdayConfigurationSnapshot(
          WorkdayConfigurationId(),
          0,
          UserId(),
          LocalDateTime.now(),
          UserId(),
          LocalDateTime.now(),
          projectIdentifier,
          MONDAY,
          setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
          setOf(Holiday("Holiday", LocalDate.now())),
          true)
}
