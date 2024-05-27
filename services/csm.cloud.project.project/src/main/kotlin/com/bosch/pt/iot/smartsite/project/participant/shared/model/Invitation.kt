/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.participant.InvitationId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID

/**
 * This class stores GDPR relevant data of an invited participant while the invitation is still
 * active. It is removed once the user become an active participant. It is also kept while the
 * participant is in status "VALIDATION" to determine the correct language for the emails. The
 * events for this are stored in a compacted topic in kafka and removed there using tombstone
 * messages.
 */
@Entity
@Table(
    indexes =
        [
            Index(name = "UK_Invitation_Identifier", columnList = "identifier", unique = true),
            Index(
                name = "UK_Invitation_Part_Identifier",
                columnList = "participant_identifier",
                unique = true),
            Index(
                name = "UK_Invitation_Email",
                columnList = "project_identifier, email",
                unique = true)])
class Invitation(

    /**
     * The participant identifier is specific enough. However, since we want to partition by project
     * the project identifier must be in present. We do not link it to the project for the same
     * reason as specified below for the participant identifier.
     */
    @AttributeOverride(
        name = "identifier", column = Column(name = "project_identifier", nullable = false))
    var projectIdentifier: ProjectId,

    /**
     * We just store the identifier of the participant to make the restore of the db easier since
     * the invitation topic contains just a few records compared to the project topic and is
     * therefore much faster reprocessed. Linking it to a participant requires the participant to
     * exist beforehand. This solution here makes eventual consistency easier.
     */
    @AttributeOverride(
        name = "identifier", column = Column(name = "participant_identifier", nullable = false))
    var participantIdentifier: ParticipantId,

    /**
     * The email address used to send an invitation email to as well as to recognize the user once
     * he signed up. If a matching invitation is found for a user that signed up, the corresponding
     * participant is linked to that user, the status of the participant changes to "in validation"
     * and this invitation is deleted.
     */
    @field:Size(min = 1, max = MAX_EMAIL_LENGTH)
    @Column(nullable = false, length = MAX_EMAIL_LENGTH)
    var email: String,

    // timestamp when invitation was triggered or resent last
    @Column(nullable = false) var lastSent: LocalDateTime,
) : AbstractSnapshotEntity<Long, InvitationId>() {

  override fun getDisplayName(): String = email

  companion object {
    const val MAX_EMAIL_LENGTH = 255
    fun newInstance() = Invitation(ProjectId(randomUUID()), ParticipantId(randomUUID()), "", now())
  }
}
