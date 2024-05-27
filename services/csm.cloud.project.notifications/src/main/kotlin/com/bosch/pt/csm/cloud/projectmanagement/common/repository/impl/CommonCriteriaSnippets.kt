/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import org.springframework.data.mongodb.core.query.Criteria

object CommonCriteriaSnippets {
  fun matchesAggregateIdentifier(aggregateIdentifier: AggregateIdentifier): Criteria =
      Criteria.where(ID_TYPE)
          .`is`(aggregateIdentifier.type)
          .and(CommonAttributeNames.ID_IDENTIFIER)
          .`is`(aggregateIdentifier.identifier)
          .and(CommonAttributeNames.ID_VERSION)
          .`is`(aggregateIdentifier.version)
}
