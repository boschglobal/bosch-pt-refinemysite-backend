/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.extension

import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectCategoryEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectCategoryEnum

fun ProjectCategoryEnumAvro.asCategory() = ProjectCategoryEnum.valueOf(this.name)
