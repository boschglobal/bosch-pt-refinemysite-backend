/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.command.dto

import com.bosch.pt.iot.smartsite.project.project.ProjectId

data class CopiedProjectResult(val projectId: ProjectId, val projectName: String)
