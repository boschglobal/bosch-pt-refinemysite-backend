/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto

import com.bosch.pt.iot.smartsite.project.project.ProjectId

class ParticipantsPerProject(val projectIdentifier: ProjectId, val numberOfParticipants: Long)
