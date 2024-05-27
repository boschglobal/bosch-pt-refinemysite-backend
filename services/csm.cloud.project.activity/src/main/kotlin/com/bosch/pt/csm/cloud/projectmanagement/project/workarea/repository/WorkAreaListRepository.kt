/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkAreaList
import org.springframework.data.mongodb.repository.MongoRepository

interface WorkAreaListRepository :
    MongoRepository<WorkAreaList, AggregateIdentifier>,
    ShardedSaveOperation<WorkAreaList, AggregateIdentifier>
