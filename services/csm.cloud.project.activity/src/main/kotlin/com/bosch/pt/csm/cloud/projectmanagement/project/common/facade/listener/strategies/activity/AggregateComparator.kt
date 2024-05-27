/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.UPDATED
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.stereotype.Component

@Component
class AggregateComparator(private val mongoOperations: MongoOperations) {
  fun <T> compare(currentVersion: T?, previousVersion: T?): MutableMap<String, AttributeChange> {

    val currentDocument = currentVersion?.let { toDocument(currentVersion) }
    val previousDocument = previousVersion?.let { toDocument(previousVersion) }

    val differences = difference(previousDocument, currentDocument)

    return differences
        .entriesDiffering()
        // attributes with a changed value
        .mapValues { AttributeChange(it.key, it.value.leftValue(), it.value.rightValue(), UPDATED) }
        // removed attributes (value is mapped to null to not have the old value in the list of
        // changes)
        .plus(
            differences.entriesOnlyOnLeft().mapValues {
              AttributeChange(it.key, it.value, null, REMOVED)
            })
        // added attributes (no mapping required, since it only contains a single value being the
        // new one)
        .plus(
            differences.entriesOnlyOnRight().mapValues {
              AttributeChange(it.key, null, it.value, CREATED)
            })
        .toMutableMap()
        // the version always differs, therefore, it is removed here
        .apply { remove("_id.version") }
  }

  private fun <T> toDocument(entity: T) =
      Document().apply { mongoOperations.converter.write(entity!!, this) }

  private fun difference(
      previousVersion: Document?,
      currentVersion: Document?
  ): MapDifference<String, Any> {
    val previousVersionMap =
        if (previousVersion == null) mapOf<String, Any>() else toMap(previousVersion, "")
    val currentVersionMap =
        if (currentVersion == null) mapOf<String, Any>() else toMap(currentVersion, "")
    return Maps.difference(previousVersionMap, currentVersionMap)
  }

  private fun toMap(document: Document, keyPrefix: String): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for (entry in document.entries) {
      val value = entry.value
      val prefixedKey = if (keyPrefix.isEmpty()) entry.key else keyPrefix + "." + entry.key

      if (value is Document) map.putAll(toMap(value, prefixedKey))
      else if (prefixedKey != "_id.version") map[prefixedKey] = value
    }
    return map
  }
}

enum class AttributeChangeEnum {
  CREATED,
  UPDATED,
  REMOVED
}

data class AttributeChange(
    val attribute: String,
    val oldValue: Any?,
    val newValue: Any?,
    val changeType: AttributeChangeEnum
)
