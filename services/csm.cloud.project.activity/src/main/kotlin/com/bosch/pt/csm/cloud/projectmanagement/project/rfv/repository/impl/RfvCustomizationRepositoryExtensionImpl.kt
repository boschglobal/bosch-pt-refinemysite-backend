/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.ID_TYPE_VALUE_RFV_CUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.model.RfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository.RfvCustomizationRepositoryExtension
import java.util.UUID
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query

open class RfvCustomizationRepositoryExtensionImpl
constructor(private val mongoOperations: MongoOperations) : RfvCustomizationRepositoryExtension {

  @Cacheable(cacheNames = ["rfv-customization"])
  override fun findLatestCachedByProjectIdentifierAndReason(
      projectIdentifier: UUID,
      reason: DayCardReasonEnum
  ): RfvCustomization? =
      mongoOperations.findOne(
          query(criteria(projectIdentifier, reason)).with(Sort.by(desc(ID_VERSION))).limit(1),
          RfvCustomization::class.java,
          PROJECT_STATE)

  private fun criteria(projectIdentifier: UUID, reason: DayCardReasonEnum) =
      Criteria(ID_TYPE)
          .`is`(ID_TYPE_VALUE_RFV_CUSTOMIZATION)
          .and(PROJECT_IDENTIFIER)
          .`is`(projectIdentifier)
          .and("reason")
          .`is`(reason)
}
