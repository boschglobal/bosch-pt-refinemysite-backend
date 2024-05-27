/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.model

import java.net.URL

data class NewQuarantineBlob(
    val uploadUrl: URL,

    /**
     * blob name within quarantine storage (including quarantine directory as prefix and a unique
     * version id as suffix)
     */
    val quarantineBlobName: String,

    /**
     * Identifies a specific version of a blob (suffix of quarantineBlobName). This makes a blob
     * name unique. Reasons see: SMAR-17544
     */
    val versionId: String,
)
