/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.request

open class VersionedUpdateBatchRequestResource(
    override val items: Collection<VersionedIdentifier>
) : UpdateBatchRequestResource<VersionedIdentifier>(items)
