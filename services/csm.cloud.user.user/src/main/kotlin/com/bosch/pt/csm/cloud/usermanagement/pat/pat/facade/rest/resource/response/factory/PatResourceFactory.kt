/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.PatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import org.springframework.stereotype.Component

@Component
class PatResourceFactory(private val patResourceFactoryHelper: PatResourceFactoryHelper) {

  fun build(pat: Pat): PatResource = patResourceFactoryHelper.build(pat)
}
