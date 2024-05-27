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
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaListSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WorkAreaListAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createWorkAreaListSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = WorkAreaListAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createWorkAreaListSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      WorkAreaListSnapshot(
          WorkAreaListId(),
          0,
          UserId(),
          LocalDateTime.now(),
          UserId(),
          LocalDateTime.now(),
          projectIdentifier,
          mutableListOf())
}
