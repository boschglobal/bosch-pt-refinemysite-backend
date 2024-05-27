/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl

import org.springframework.data.mongodb.core.query.Criteria

object CriteriaOperator {

    fun and(first: Criteria, second: Criteria): Criteria {
        return Criteria().andOperator(first, second)
    }

    fun or(first: Criteria, second: Criteria): Criteria {
        return Criteria().orOperator(first, second)
    }
}
