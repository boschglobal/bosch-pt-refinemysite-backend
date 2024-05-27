/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.response

abstract class AbstractPageResource(
    pageNumber: Int,
    pageSize: Int,
    val totalPages: Int,
    val totalElements: Long
) : AbstractSliceResource(pageNumber, pageSize)
