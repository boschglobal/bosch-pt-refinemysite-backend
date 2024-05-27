/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import java.util.UUID

fun WorkAreaAggregateAvro.toEntity(projectIdentifier: UUID) =
    WorkArea(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        name = getName())
