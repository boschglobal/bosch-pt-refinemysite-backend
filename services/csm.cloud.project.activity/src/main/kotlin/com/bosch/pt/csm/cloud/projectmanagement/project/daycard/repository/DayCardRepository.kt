/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCard
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface DayCardRepository :
    MongoRepository<DayCard, AggregateIdentifier>,
    ShardedSaveOperation<DayCard, AggregateIdentifier>,
    ProjectContextOperationsExtension<DayCard> {

  @Query("{'_class': DayCard}") override fun findAll(): List<DayCard>

  @DeleteQuery("{'_class': DayCard}") override fun deleteAll()
}
