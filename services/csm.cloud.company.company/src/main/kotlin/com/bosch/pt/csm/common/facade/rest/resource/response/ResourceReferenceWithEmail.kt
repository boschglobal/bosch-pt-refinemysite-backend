/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.util.UUID

class ResourceReferenceWithEmail(identifier: UUID, displayName: String, val email: String) :
    ResourceReference(identifier, displayName)
