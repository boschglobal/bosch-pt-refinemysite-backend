/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import java.util.UUID
import jakarta.validation.constraints.Size

class AssignTaskListToParticipantResource(
    @field:Size(min = 1, max = 100) val taskIds: List<UUID>,
    assigneeId: ParticipantId
) : AssignTaskToParticipantResource(assigneeId = assigneeId)
