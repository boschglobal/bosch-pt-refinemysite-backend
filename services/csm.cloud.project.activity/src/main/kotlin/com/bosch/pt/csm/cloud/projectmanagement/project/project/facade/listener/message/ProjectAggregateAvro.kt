/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAddressAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.ProjectAddress
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.ProjectCategoryEnum

fun ProjectAggregateAvro.toEntity() =
    Project(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = getIdentifier(),
        title = getTitle(),
        description = getDescription(),
        start = getStart().toLocalDateByMillis(),
        end = getEnd().toLocalDateByMillis(),
        client = getClient(),
        projectNumber = getProjectNumber(),
        category = getCategory()?.run { ProjectCategoryEnum.valueOf(getCategory().name) },
        projectAddress = getProjectAddress().toEntity())

private fun ProjectAddressAvro.toEntity() =
    ProjectAddress(
        city = getCity(),
        houseNumber = getHouseNumber(),
        street = getStreet(),
        zipCode = getZipCode())
