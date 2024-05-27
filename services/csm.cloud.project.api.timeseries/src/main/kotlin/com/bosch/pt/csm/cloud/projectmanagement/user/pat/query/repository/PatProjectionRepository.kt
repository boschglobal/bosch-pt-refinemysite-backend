/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.PatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import org.springframework.data.mongodb.repository.MongoRepository

interface PatProjectionRepository : MongoRepository<PatProjection, PatId> {

  fun findOneByIdentifier(identifier: PatId): PatProjection?

  fun findAllByImpersonatedUserIdentifier(identifier: UserId): List<PatProjection>
}
