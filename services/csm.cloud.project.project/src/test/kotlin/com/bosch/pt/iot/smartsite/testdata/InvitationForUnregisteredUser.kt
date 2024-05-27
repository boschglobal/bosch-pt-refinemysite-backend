/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.testdata

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3

fun EventStreamGenerator.invitationForUnregisteredUser(
    referencePrefix: String = "unregistered-user",
    projectReference: String = "project",
    participantRole: ParticipantRoleEnumAvro = FM
): EventStreamGenerator {
  submitParticipantG3(asReference = "$referencePrefix-participant") {
    it.project = getByReference(projectReference)
    it.company = null
    it.user = null
    it.status = INVITED
    it.role = participantRole
  }
  submitInvitation(asReference = "$referencePrefix-invitation") {
    it.participantIdentifier = getIdentifier("$referencePrefix-participant").toString()
    it.projectIdentifier = getIdentifier(projectReference).toString()
  }
  return this
}
