/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getCompanyIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.ParticipantRoleEnum

fun ParticipantAggregateG3Avro.toEntity(isActive: Boolean) =
    Participant(
        identifier = getIdentifier(),
        role = ParticipantRoleEnum.valueOf(getRole().name),
        projectIdentifier = getProjectIdentifier(),
        companyIdentifier = getCompanyIdentifier()!!,
        userIdentifier = getUserIdentifier()!!,
        active = isActive)
