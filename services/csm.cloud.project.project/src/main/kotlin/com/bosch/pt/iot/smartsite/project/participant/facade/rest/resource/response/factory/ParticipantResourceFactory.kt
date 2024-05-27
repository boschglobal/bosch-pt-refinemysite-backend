/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantResource
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import org.springframework.stereotype.Component

@Component
open class ParticipantResourceFactory(
    private val resourceFactoryHelper: ParticipantResourceFactoryHelper
) {

  open fun build(participant: Participant): ParticipantResource =
      resourceFactoryHelper.build(participant.project!!.identifier, listOf(participant)).first()
}
