/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import org.springframework.stereotype.Component

@Component
open class RelationBatchResourceFactory(private val factoryHelper: RelationResourceFactoryHelper) {

  open fun build(relations: List<Relation>, projectId: ProjectId) =
      BatchResponseResource(items = factoryHelper.build(relations))
}
