@file:Suppress("MatchingDeclarationName", "Filename")
/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.craft.api

import com.bosch.pt.csm.cloud.usermanagement.common.model.Translation
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId

data class CreateCraftCommand(
    val identifier: CraftId,
    val defaultName: String,
    val translations: Collection<Translation> = ArrayList()
)
