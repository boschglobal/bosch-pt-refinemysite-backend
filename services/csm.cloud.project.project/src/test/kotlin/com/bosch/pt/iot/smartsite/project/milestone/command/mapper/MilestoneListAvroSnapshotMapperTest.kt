/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MilestoneListAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createMilestoneListSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = MilestoneListAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createMilestoneListSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      MilestoneListSnapshot(
          MilestoneListId(),
          0,
          UserId(),
          LocalDateTime.now(),
          UserId(),
          LocalDateTime.now(),
          projectIdentifier,
          LocalDate.now(),
          false,
          WorkAreaId(),
          mutableListOf())
}
