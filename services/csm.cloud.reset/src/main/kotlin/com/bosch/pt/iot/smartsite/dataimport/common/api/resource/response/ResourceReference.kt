/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response

import java.util.UUID

open class ResourceReference(val displayName: String? = null, var id: UUID? = null)
