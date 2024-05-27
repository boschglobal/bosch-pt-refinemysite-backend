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
import org.springframework.core.io.Resource

class ProjectPicture(
    override val id: String,
    val version: Long? = null,
    val projectId: String,
    val path: String,
    var resource: Resource? = null
) : UserBasedImport(), ImportObject
