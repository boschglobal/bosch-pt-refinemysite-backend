/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.participant.InvitationId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshot
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InvitationAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the invitation identifier`() {
    val identifier = InvitationId()
    val snapshot = createInvitationSnapshot(identifier = identifier)

    val messageKey = InvitationAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(identifier.toUuid())
  }

  private fun createInvitationSnapshot(identifier: InvitationId = InvitationId()) =
      InvitationSnapshot(
          identifier, 0, UserId(), now(), UserId(), now(), ProjectId(), ParticipantId(), "email")
}
