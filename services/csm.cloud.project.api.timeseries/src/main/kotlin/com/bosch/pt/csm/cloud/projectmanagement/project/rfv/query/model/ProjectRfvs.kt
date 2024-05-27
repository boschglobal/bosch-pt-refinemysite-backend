/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId

class ProjectRfvs(val projectId: ProjectId, val rfvs: List<Rfv>)
