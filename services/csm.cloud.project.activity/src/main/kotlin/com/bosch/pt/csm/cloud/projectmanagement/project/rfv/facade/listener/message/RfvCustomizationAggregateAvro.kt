/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.model.RfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro

fun RfvCustomizationAggregateAvro.toEntity() =
    RfvCustomization(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = getProjectIdentifier(),
        DayCardReasonEnum.valueOf(key.name),
      active,
      name
    )
