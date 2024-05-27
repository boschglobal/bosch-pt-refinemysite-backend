/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshot
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.NB
import java.time.LocalDate.now
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProjectAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createProjectSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = ProjectAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createProjectSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      ProjectSnapshot(
          projectIdentifier,
          0,
          UserId(),
          LocalDateTime.now(),
          UserId(),
          LocalDateTime.now(),
          "client",
          "description",
          now(),
          now().plusMonths(1),
          "1",
          "title",
          NB,
          ProjectAddressVo("street", "1", "city", "zip"))
}
