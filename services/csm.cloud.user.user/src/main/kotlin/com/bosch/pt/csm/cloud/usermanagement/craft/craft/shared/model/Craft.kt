/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.referencedata.craft.common.CraftAggregateTypeEnum.CRAFT
import com.bosch.pt.csm.cloud.usermanagement.common.model.Translation
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
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

@Entity
@Table(
    name = "craft",
    indexes = [Index(name = "UK_Craft_Identifier", columnList = "identifier", unique = true)])
class Craft : AbstractSnapshotEntity<Long, CraftId> {

  @Column(nullable = false, length = MAX_CRAFT_NAME_LENGTH)
  var defaultName: @NotNull @Size(min = 1, max = MAX_CRAFT_NAME_LENGTH) String? = null

  @ElementCollection
  @CollectionTable(
      name = "CRAFT_TRANSLATION",
      uniqueConstraints =
          [
              UniqueConstraint(
                  name = "UK_CRAFT_TRANSLATION_LANG", columnNames = ["craft_id", "locale"])],
      foreignKey = ForeignKey(name = "FK_Craft_Translation_CraftId"),
      joinColumns = [JoinColumn(name = "CRAFT_ID")])
  var translations: MutableList<Translation> = ArrayList()

  /** For JPA. */
  constructor() {
    // default constructor
  }

  /**
   * Field initialising constructor.
   *
   * @param identifier the identifier of the craft
   * @param defaultName the name of the craft in the default language
   * @param translations the translations of the name of the craft
   */
  constructor(identifier: CraftId, defaultName: String, translations: Set<Translation>) {
    this.identifier = identifier
    this.defaultName = defaultName
    this.translations.addAll(translations)
  }

  override fun getDisplayName(): String? = defaultName

  fun asAggregateIdentifier() = AggregateIdentifier(CRAFT.name, getIdentifierUuid(), version)

  companion object {
    const val MAX_LANGUAGE_LENGTH = 3
    const val MAX_CRAFT_NAME_LENGTH = 128
  }
}
