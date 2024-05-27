/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTypeEnum.NEUTRAL
import com.bosch.pt.csm.cloud.usermanagement.common.model.Translation
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID

class AnnouncementBuilder private constructor() {

  var identifier: UUID = randomUUID()
  var type: AnnouncementTypeEnum = NEUTRAL
  var translations: MutableList<Translation> =
      mutableListOf(
          Translation(Locale.ENGLISH.language, randomUUID().toString()),
          Translation(Locale.GERMAN.language, randomUUID().toString()))

  fun build() = Announcement(identifier, type, translations)

  companion object {
    fun announcement() = AnnouncementBuilder()
  }
}
