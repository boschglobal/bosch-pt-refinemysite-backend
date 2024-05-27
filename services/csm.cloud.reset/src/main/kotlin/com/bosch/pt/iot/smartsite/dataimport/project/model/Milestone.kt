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

data class Milestone(
    override val id: String,
    val version: Long? = null,
    val name: String? = null,
    val type: MilestoneTypeEnum? = null,
    val date: Int,
    val header: Boolean = false,
    val projectId: String,
    val description: String? = null,
    val craftId: String? = null,
    val workAreaId: String? = null,
    override val createWithUserId: String
) : UserBasedImport(), ImportObject
