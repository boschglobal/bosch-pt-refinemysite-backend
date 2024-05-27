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
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface WorkAreaRepository :
    MongoRepository<WorkArea, AggregateIdentifier>,
    ShardedSaveOperation<WorkArea, AggregateIdentifier>,
    ProjectContextOperationsExtension<WorkArea> {

  @Query("{'_class': WorkArea}") override fun findAll(): List<WorkArea>

  @DeleteQuery("{'_class': WorkArea}") override fun deleteAll()
}
