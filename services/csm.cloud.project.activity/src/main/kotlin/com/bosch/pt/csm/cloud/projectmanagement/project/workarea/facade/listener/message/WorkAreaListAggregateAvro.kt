/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro

fun WorkAreaListAggregateAvro.toEntity() =
    WorkAreaList(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = getProjectIdentifier(),
        workAreas = getWorkAreas().toIdentifiers())

private fun Collection<AggregateIdentifierAvro>.toIdentifiers() = map {
  it.getIdentifier().toUUID()
}
