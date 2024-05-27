/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.common.model.UserBasedImport

class ProjectParticipant(
    override val id: String,
    val version: Long? = null,
    val email: String? = null,
    val projectId: String,
    val role: ProjectRoleEnum? = null
) : UserBasedImport(), ImportObject
