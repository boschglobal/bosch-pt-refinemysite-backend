/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.FeatureId
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.shared.model.WhitelistedSubject
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeatureStateEnum.WHITELIST_ACTIVATED
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.Size

@Entity
@Table(
    indexes =
        [
            Index(name = "UK_Feature_Name", columnList = "name", unique = true),
        ])
class Feature : AbstractSnapshotEntity<Long, FeatureId>() {

  @field:Size(max = MAX_NAME_LENGTH) // name field
  @Column(length = MAX_NAME_LENGTH, nullable = false)
  lateinit var name: String

  @Column(nullable = false, columnDefinition = "varchar(255)") // state field
  @Enumerated(EnumType.STRING)
  var state: FeatureStateEnum = WHITELIST_ACTIVATED

  @ElementCollection // whitelistedSubjects field
  @CollectionTable(
      name = "FEATURE_WHITELISTED_SUBJECT",
      foreignKey = ForeignKey(name = "FK_Feature_WhitelistedSubject_FeatureId"),
      joinColumns = [JoinColumn(name = "FEATURE_ID")])
  var whitelistedSubjects: MutableSet<WhitelistedSubject> = mutableSetOf()

  override fun getDisplayName(): String = name

  companion object {
    private const val MAX_NAME_LENGTH = 50
  }
}
