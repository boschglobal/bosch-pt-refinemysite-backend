/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParticipantAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createParticipantSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = ParticipantAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createParticipantSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      ParticipantSnapshot(
          ParticipantId(),
          0,
          UserId(),
          now(),
          UserId(),
          now(),
          projectIdentifier,
          CompanyId(),
          UserId(),
          CSM,
          ACTIVE)
}
