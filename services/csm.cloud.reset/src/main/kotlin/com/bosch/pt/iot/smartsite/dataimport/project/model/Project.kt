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

data class Project(
    override val id: String,
    val version: Long? = null,
    val constructionSiteManagerId: String? = null,
    val client: String? = null,
    val description: String? = null,
    val end: Int,
    val start: Int,
    val projectNumber: String? = null,
    val title: String? = null,
    val category: ProjectCategoryEnum? = null,
    val address: ProjectAddress? = null,
    val constructionSiteManager: ProjectConstructionSiteManager? = null,
    override val createWithUserId: String?
) : UserBasedImport(), ImportObject
