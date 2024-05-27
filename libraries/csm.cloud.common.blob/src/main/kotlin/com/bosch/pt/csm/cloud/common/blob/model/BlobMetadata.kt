/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.model

import com.google.common.collect.Maps
import java.util.Collections
import java.util.TimeZone

class BlobMetadata private constructor() {

  private val metadata = mutableMapOf<String, String>()

  fun get(key: String): String? {
    return metadata[key]
  }

  fun putAll(map: Map<String, String>) {
    metadata.putAll(map)
  }

  fun put(key: String, value: String): String? {
    return metadata.put(key, value)
  }

  fun containsKey(key: String): Boolean {
    return metadata.containsKey(key)
  }

  fun toMap(): Map<String, String> {
    return Collections.unmodifiableMap(metadata)
  }

  enum class OwnerType {
    TASK_ATTACHMENT,
    TOPIC_ATTACHMENT,
    MESSAGE_ATTACHMENT,
    PROJECT_PICTURE,
    USER_PICTURE
  }

  companion object {

    const val KEY_OWNER_IDENTIFIER =
        "owner_identifier" // the identifier of the entity the Blob belongs to
    const val KEY_OWNER_TYPE = "owner_type" // the type of the entity this Blob belongs to
    const val KEY_TIMEZONE = "timezone"
    const val KEY_FILENAME = "filename" // the original file name before storing the file as a Blob

    fun from(fileName: String, captureTimeZone: TimeZone, blobOwner: BlobOwner): BlobMetadata {
      val metadata: MutableMap<String, String> = Maps.newHashMap()
      metadata[KEY_OWNER_IDENTIFIER] = blobOwner.getIdentifierUuid().toString()
      metadata[KEY_OWNER_TYPE] = blobOwner.getOwnerType().name
      metadata[KEY_FILENAME] = fileName
      metadata[KEY_TIMEZONE] = captureTimeZone.id
      return fromMap(metadata)
    }

    fun fromMap(map: Map<String, String>? = emptyMap()): BlobMetadata =
        BlobMetadata().apply { putAll(checkNotNull(map)) }
  }
}
