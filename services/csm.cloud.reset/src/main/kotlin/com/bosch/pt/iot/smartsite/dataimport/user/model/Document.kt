/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.dataimport.user.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import java.time.Instant

data class Document(
    val identifier: String,
    val type: String,
    val country: String,
    val locale: String,
    val client: String,
    val displayName: String,
    val url: String,
    val versions: List<DocumentVersion>,
    override val id: String = identifier
) : ImportObject

data class DocumentVersion(val identifier: String, val lastChanged: Instant)
