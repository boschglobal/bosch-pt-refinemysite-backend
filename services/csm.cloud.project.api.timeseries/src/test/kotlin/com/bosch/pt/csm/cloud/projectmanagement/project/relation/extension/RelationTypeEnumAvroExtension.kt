/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.extension

import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro

fun RelationTypeEnumAvro.toRelationType() = RelationTypeEnum.valueOf(this.name)
