/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WorkAreaAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createWorkAreaSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = WorkAreaAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createWorkAreaSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      WorkAreaSnapshot(
          WorkAreaId(),
          0,
          UserId(),
          LocalDateTime.now(),
          UserId(),
          LocalDateTime.now(),
          "name",
          null,
          projectIdentifier)
}
