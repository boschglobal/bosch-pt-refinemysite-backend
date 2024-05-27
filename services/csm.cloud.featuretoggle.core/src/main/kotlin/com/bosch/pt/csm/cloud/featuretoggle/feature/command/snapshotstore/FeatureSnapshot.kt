/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model.Feature
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.shared.model.WhitelistedSubject
import java.time.LocalDateTime

data class FeatureSnapshot(
    override val identifier: FeatureId,
    override val version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override val createdDate: LocalDateTime? = null,
    override val createdBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    val name: String,
    val whitelistedSubjects: MutableSet<WhitelistedSubject> = mutableSetOf()
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      feature: Feature
  ) : this(
      requireNotNull(feature.identifier),
      feature.version,
      feature.createdDate.get(),
      feature.createdBy.get(),
      feature.lastModifiedDate.get(),
      feature.lastModifiedBy.get(),
      feature.name,
      feature.whitelistedSubjects)
}

fun Feature.asValueObject() = FeatureSnapshot(this)
