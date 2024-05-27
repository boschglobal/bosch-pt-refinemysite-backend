/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.common.model.UserBasedImport
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TaskStatusEnum

data class Task(
    override val id: String,
    val version: Long? = null,
    val projectId: String,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val work: String? = null,
    val start: Int? = null,
    val end: Int? = null,
    val status: TaskStatusEnum,
    val assigneeId: String? = null,
    val projectCraftId: String,
    val workAreaId: String? = null,
    override val createWithUserId: String
) : UserBasedImport(), ImportObject
