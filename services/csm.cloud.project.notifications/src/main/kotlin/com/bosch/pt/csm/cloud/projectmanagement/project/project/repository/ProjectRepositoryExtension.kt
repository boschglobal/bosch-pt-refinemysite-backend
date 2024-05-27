/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

interface ProjectRepositoryExtension {

    fun findLatest(identifier: UUID): Project

    @Cacheable(cacheNames = ["project-display-name"])
    fun findDisplayName(identifier: UUID): String

    fun deleteProjectAndAllRelatedDocuments(identifier: UUID)
}
