/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MilestoneAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createMilestoneSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = MilestoneAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createMilestoneSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      MilestoneSnapshot(
          MilestoneId(),
          0,
          UserId(),
          LocalDateTime.now(),
          UserId(),
          LocalDateTime.now(),
          "name",
          PROJECT,
          LocalDate.now(),
          false,
          projectIdentifier,
          ProjectCraftId(),
          WorkAreaId(),
          "description")
}
