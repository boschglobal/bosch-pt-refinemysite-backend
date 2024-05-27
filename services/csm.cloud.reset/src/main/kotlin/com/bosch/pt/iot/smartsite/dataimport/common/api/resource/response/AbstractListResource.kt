/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response

import org.springframework.hateoas.RepresentationModel

abstract class AbstractListResource(
    val pageNumber: Long = 0,
    val pageSize: Long = 0,
    val totalPages: Long = 0,
    val totalElements: Long = 0
) : RepresentationModel<AbstractListResource>()
