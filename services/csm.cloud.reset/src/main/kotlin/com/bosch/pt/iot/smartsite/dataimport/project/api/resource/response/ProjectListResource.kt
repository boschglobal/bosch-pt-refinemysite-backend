/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.hateoas.RepresentationModel

@JsonIgnoreProperties("_links")
class ProjectListResource(val projects: Collection<ProjectResource> = emptyList()) :
    RepresentationModel<ProjectListResource?>()
