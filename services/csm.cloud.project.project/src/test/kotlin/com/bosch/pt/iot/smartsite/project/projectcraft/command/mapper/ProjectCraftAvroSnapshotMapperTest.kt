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
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftSnapshot
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectCraftAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createProjectCraftSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = ProjectCraftAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createProjectCraftSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      ProjectCraftSnapshot(
          ProjectCraftId(), 0, UserId(), now(), UserId(), now(), projectIdentifier, "name", "color")
}
