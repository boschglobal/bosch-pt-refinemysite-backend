/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.repository.dto

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum

data class RelationFilterDto(
    val types: Set<RelationTypeEnum> = emptySet(),
    val sources: Set<RelationElement> = emptySet(),
    val targets: Set<RelationElement> = emptySet(),
    val projectIdentifier: ProjectId
)
