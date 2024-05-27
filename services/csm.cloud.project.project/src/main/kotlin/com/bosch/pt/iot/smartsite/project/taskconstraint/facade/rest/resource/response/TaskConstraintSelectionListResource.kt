/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import org.springframework.hateoas.RepresentationModel

class TaskConstraintSelectionListResource(val selections: List<TaskConstraintSelectionResource>) :
    RepresentationModel<AbstractResource>()
