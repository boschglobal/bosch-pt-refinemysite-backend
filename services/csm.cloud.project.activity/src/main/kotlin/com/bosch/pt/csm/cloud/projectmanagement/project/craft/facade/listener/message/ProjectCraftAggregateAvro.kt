/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.craft.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.model.ProjectCraft
import java.util.UUID

fun ProjectCraftAggregateG2Avro.toEntity(projectIdentifier: UUID) =
    ProjectCraft(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        color = getColor(),
        name = getName())
