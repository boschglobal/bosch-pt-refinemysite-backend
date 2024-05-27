/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractPageResource

class ParticipantListResource(
    val items: List<ParticipantResource>,
    pageNumber: Int,
    pageSize: Int,
    totalPages: Int,
    totalElements: Long
) : AbstractPageResource(pageNumber, pageSize, totalPages, totalElements)
