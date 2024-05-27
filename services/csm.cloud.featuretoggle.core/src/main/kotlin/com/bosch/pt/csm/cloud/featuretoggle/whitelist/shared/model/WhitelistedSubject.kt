/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.shared.model

import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.io.Serializable
import java.util.UUID

@Embeddable
@Table(
    indexes =
        [
            Index(name = "UK_Whitelist_SbjName", columnList = "featureName,subject", unique = true),
            Index(name = "IX_Whitelist_FeatureName", columnList = "featureName"),
        ])
class WhitelistedSubject() : Serializable {

  @Column(nullable = false) // subjectRef field
  lateinit var subjectRef: UUID

  @Column(nullable = false, columnDefinition = "varchar(255)") // type field
  @Enumerated(EnumType.STRING)
  lateinit var type: SubjectTypeEnum

  @field:Size(max = MAX_NAME_LENGTH) // featureName field
  @Column(length = MAX_NAME_LENGTH)
  lateinit var featureName: String

  constructor(subjectRef: UUID, type: SubjectTypeEnum, featureName: String) : this() {
    this.subjectRef = subjectRef
    this.type = type
    this.featureName = featureName
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is WhitelistedSubject) return false
    if (subjectRef != other.subjectRef) return false
    if (type != other.type) return false
    if (featureName != other.featureName) return false

    return true
  }

  override fun hashCode(): Int {
    var result = subjectRef.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + featureName.hashCode()
    return result
  }

  companion object {
    private const val MAX_NAME_LENGTH = 50
    private const val serialVersionUID = -23975L
  }
}
