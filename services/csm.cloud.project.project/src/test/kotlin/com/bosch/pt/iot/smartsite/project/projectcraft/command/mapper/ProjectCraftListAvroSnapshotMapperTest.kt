/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftListSnapshot
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectCraftListAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createProjectCraftListSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = ProjectCraftListAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createProjectCraftListSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      ProjectCraftListSnapshot(
          ProjectCraftListId(),
          0,
          UserId(),
          now(),
          UserId(),
          now(),
          projectIdentifier,
          mutableListOf())
}
