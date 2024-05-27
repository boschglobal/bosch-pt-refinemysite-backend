/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCard
import org.springframework.data.mongodb.repository.MongoRepository

interface DayCardRepository :
    MongoRepository<DayCard, AggregateIdentifier>,
    ShardedSaveOperation<DayCard, AggregateIdentifier>,
    DayCardRepositoryExtension
