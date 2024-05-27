/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import org.springframework.data.mongodb.repository.MongoRepository

interface WorkAreaRepository :
    MongoRepository<WorkArea, AggregateIdentifier>,
    ShardedSaveOperation<WorkArea, AggregateIdentifier>,
    WorkAreaRepositoryExtension
