package com.bosch.pt.csm.cloud.projectmanagement.event.model

import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import java.util.UUID

class ObjectIdentifierWithVersion(type: String, identifier: UUID, val version: Long) :
    ObjectIdentifier(type, identifier) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ObjectIdentifierWithVersion

    if (version != other.version) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + version.hashCode()
    return result
  }
}
