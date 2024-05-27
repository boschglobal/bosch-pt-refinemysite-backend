/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.model

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.common.CraftAggregateTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.common.model.Translation
import com.bosch.pt.iot.smartsite.user.model.User
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_Craft_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_Craft_LastModifiedBy")))
@Table(
    name = "craft",
    indexes = [Index(name = "UK_Craft_Identifier", columnList = "identifier", unique = true)])
class Craft : AbstractReplicatedEntity<Long> {

  @field:NotNull
  @field:Size(min = 1, max = MAX_CRAFT_NAME_LENGTH)
  @Column(length = MAX_CRAFT_NAME_LENGTH, nullable = false)
  var defaultName: String? = null

  @ElementCollection
  @CollectionTable(
      name = "CRAFT_TRANSLATION",
      uniqueConstraints =
          [
              UniqueConstraint(
                  name = "UK_CRAFT_TRANSLATION_LANG", columnNames = ["craft_id", "locale"])],
      foreignKey = ForeignKey(name = "FK_Craft_Translation_CraftId"),
      joinColumns = [JoinColumn(name = "CRAFT_ID")])
  var translations: MutableList<Translation> = mutableListOf()

  /** For JPA. */
  constructor()

  constructor(
      identifier: UUID?,
      version: Long?,
      defaultName: String?,
      translations: Set<Translation>?
  ) {
    this.identifier = identifier
    this.version = version
    this.defaultName = defaultName
    this.translations.addAll(requireNotNull(translations))
  }

  override fun getDisplayName(): String? = defaultName

  override fun getAggregateType(): String = CRAFT.value

  companion object {
    const val MAX_LANGUAGE_LENGTH = 3
    const val MAX_CRAFT_NAME_LENGTH = 128

    @JvmStatic
    fun fromAvroMessage(
        aggregate: CraftAggregateAvro,
        createdBy: User,
        lastModifiedBy: User
    ): Craft {

      val translations =
          aggregate.getTranslations().map { Translation(it.getLocale(), it.getValue()) }.toSet()

      return Craft(
              aggregate.getAggregateIdentifier().getIdentifier().toUUID(),
              aggregate.getAggregateIdentifier().getVersion(),
              aggregate.getDefaultName(),
              translations)
          .apply {
            setCreatedBy(createdBy)
            setLastModifiedBy(lastModifiedBy)
            setCreatedDate(
                aggregate.getAuditingInformation().getCreatedDate().toLocalDateTimeByMillis())
            setLastModifiedDate(
                aggregate.getAuditingInformation().getLastModifiedDate().toLocalDateTimeByMillis())
          }
    }
  }
}
