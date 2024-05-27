/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.PatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import org.springframework.stereotype.Component

@Component
class PatBatchResourceFactory(private val patResourceFactoryHelper: PatResourceFactoryHelper) {

  fun build(pats: List<Pat>): BatchResponseResource<PatResource> =
      BatchResponseResource(patResourceFactoryHelper.buildResources(pats))
}
