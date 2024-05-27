/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.extension

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.MilestoneTypeEnum

fun MilestoneTypeEnumAvro.asMilestoneType() = MilestoneTypeEnum.valueOf(this.name)
