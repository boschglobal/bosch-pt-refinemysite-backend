/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.query

import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import java.util.Locale
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "UserProjection")
@TypeAlias("UserProjection")
data class UserProjection(
    @Id val userIdentifier: UserIdentifier,
    @Indexed val externalUserIdentifier: ExternalUserIdentifier,
    val locale: Locale?
)
