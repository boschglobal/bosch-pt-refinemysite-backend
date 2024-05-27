/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import org.springframework.stereotype.Component

@Component
open class RelationResourceFactory(private val resourceFactoryHelper: RelationResourceFactoryHelper) {

  open fun build(relation: Relation) = resourceFactoryHelper.build(listOf(relation)).single()
}
