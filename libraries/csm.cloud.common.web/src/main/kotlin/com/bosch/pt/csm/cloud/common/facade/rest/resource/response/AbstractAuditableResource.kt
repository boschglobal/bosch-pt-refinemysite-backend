/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest.resource.response

import java.util.Date
import java.util.UUID

abstract class AbstractAuditableResource(
    open val id: UUID,
    open val version: Long,
    open val createdDate: Date,
    open val createdBy: ResourceReference,
    open val lastModifiedDate: Date,
    open val lastModifiedBy: ResourceReference
) : AbstractResource()
