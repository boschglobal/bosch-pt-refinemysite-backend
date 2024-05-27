/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.model

import com.bosch.pt.iot.smartsite.common.model.Translation
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime.now
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID

class CraftBuilder private constructor() {
  private val createdDate = now()
  private val lastModifiedDate = now()
  private val translations: MutableSet<Translation> = mutableSetOf()
  private var defaultName: String? = null
  private var identifier: UUID? = null
  private var version: Long? = null
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null

  fun withDefaultName(defaultName: String?): CraftBuilder = apply { this.defaultName = defaultName }

  fun withIdentifier(identifier: UUID?): CraftBuilder = apply { this.identifier = identifier }

  fun withVersion(version: Long?): CraftBuilder = apply { this.version = version }

  fun addTranslation(locale: Locale, translation: String): CraftBuilder = apply {
    this.translations.add(Translation(locale.language, translation))
  }

  fun withCreatedBy(createdBy: User?): CraftBuilder = apply { this.createdBy = createdBy }

  fun withLastModifiedBy(lastModifiedBy: User?): CraftBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withTranslations(translations: Set<Translation>): CraftBuilder = apply {
    this.translations.addAll(translations)
  }

  fun build(): Craft {
    val craft = Craft(identifier, version, defaultName, translations)
    craft.setCreatedBy(createdBy)
    craft.setLastModifiedBy(lastModifiedBy)
    craft.setCreatedDate(createdDate)
    craft.setLastModifiedDate(lastModifiedDate)
    return craft
  }

  companion object {
    @JvmStatic
    fun craft(): CraftBuilder =
        CraftBuilder().withIdentifier(randomUUID()).withVersion(0L).withDefaultName("Elektrizität")
  }
}
