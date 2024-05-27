/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import org.springframework.data.mongodb.repository.MongoRepository

interface ProjectRepository :
    MongoRepository<Project, AggregateIdentifier>,
    ShardedSaveOperation<Project, AggregateIdentifier>,
    ProjectRepositoryExtension
