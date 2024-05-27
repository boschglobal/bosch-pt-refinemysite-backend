/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.api.LocalEntity
import com.bosch.pt.csm.cloud.usermanagement.common.model.Translation
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import java.util.Locale
import java.util.UUID
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@Table(
    name = "announcement",
    indexes =
        [Index(name = "UK_Announcement_Identifier", columnList = "identifier", unique = true)])
class Announcement(

    // identifier
    @field:NotNull @Column(nullable = false) val identifier: UUID,

    // type
    @field:NotNull @Column(nullable = false) val type: AnnouncementTypeEnum,

    // translations
    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = "announcement_translation",
        uniqueConstraints =
            [
                UniqueConstraint(
                    name = "UK_Announcement_Translation_Lang",
                    columnNames = ["announcement_id", "locale"])],
        foreignKey = ForeignKey(name = "FK_Announcement_Translation_AnnouncementId"),
        joinColumns = [JoinColumn(name = "ANNOUNCEMENT_ID")])
    val translations: List<Translation> = ArrayList()
) : LocalEntity<Long>() {

  fun getTranslationByLocale(locale: Locale): AnnouncementTranslationProjection? =
      translations
          .firstOrNull { it.locale == locale.language }
          ?.let { AnnouncementTranslationProjection(this.identifier, this.type, it.value!!) }

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Announcement) return false
    if (!super.equals(other)) return false

    if (identifier != other.identifier) return false
    if (type != other.type) return false
    if (translations != other.translations) return false

    return true
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + identifier.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + translations.hashCode()
    return result
  }

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this)
          .apply {
            appendSuper(super.toString())
            append("identifier", identifier)
            append("type", type)
            append("translations", translations)
          }
          .toString()
}
