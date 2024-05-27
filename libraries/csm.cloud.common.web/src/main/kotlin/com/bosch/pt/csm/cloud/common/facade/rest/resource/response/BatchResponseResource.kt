/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.response

data class BatchResponseResource<T>(val items: Collection<T> = emptyList()) : AbstractResource()
