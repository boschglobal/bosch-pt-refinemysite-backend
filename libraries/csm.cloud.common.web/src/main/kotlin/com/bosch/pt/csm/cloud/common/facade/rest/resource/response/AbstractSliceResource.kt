/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.response

import org.springframework.hateoas.RepresentationModel

abstract class AbstractSliceResource(val pageNumber: Int, val pageSize: Int) :
    RepresentationModel<AbstractResource>()
