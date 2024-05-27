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

data class WorkArea(
    override val id: String,
    val version: Long? = null,
    val projectId: String,
    val name: String,
    val position: Int? = null,
    val etag: String,
    override val createWithUserId: String
) : UserBasedImport(), ImportObject
