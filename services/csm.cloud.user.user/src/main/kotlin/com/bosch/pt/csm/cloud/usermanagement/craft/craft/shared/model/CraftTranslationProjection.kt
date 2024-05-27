/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model

import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId

data class CraftTranslationProjection(
    val craftId: CraftId,
    val defaultName: String,

    /**
     * the locale of the translation; null, if no translation was found for the requested locale.
     */
    val locale: String?,

    /** the translated craft name; null, if no translation was found for the requested locale. */
    val value: String? // nullable, see above
)
