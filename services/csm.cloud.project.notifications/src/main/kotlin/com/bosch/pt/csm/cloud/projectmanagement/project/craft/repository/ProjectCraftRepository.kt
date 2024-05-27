/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.model.ProjectCraft
import org.springframework.data.mongodb.repository.MongoRepository

interface ProjectCraftRepository :
    MongoRepository<ProjectCraft, AggregateIdentifier>,
    ShardedSaveOperation<ProjectCraft, AggregateIdentifier>,
    ProjectCraftRepositoryExtension
