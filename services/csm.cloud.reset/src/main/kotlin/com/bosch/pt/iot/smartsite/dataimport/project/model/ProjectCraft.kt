/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.common.model.UserBasedImport

class ProjectCraft(
    override val id: String,
    val version: Long? = null,
    val projectId: String,
    val name: String? = null,
    val color: String? = null,
    val etag: String
) : UserBasedImport(), ImportObject
