/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractBlobStorageProperties(
    open val connectionString: String,
    open val corsEnabled: Boolean = false,
    open val containerName: String,
    open val sharedAccessExpiryTimeSeconds: Long = 60L
)

@ConfigurationProperties(prefix = "custom.blob-storage-downloads")
data class DownloadBlobStorageProperties(
    override val connectionString: String,
    override val corsEnabled: Boolean = false,
    override val containerName: String = "downloads",
    override val sharedAccessExpiryTimeSeconds: Long = 60L
) :
    AbstractBlobStorageProperties(
        connectionString, corsEnabled, connectionString, sharedAccessExpiryTimeSeconds)

@ConfigurationProperties(prefix = "custom.blob-storage-imports")
data class ImportBlobStorageProperties(
    override val connectionString: String,
    override val corsEnabled: Boolean = false,
    override val containerName: String = "imports",
    override val sharedAccessExpiryTimeSeconds: Long = 60L
) :
    AbstractBlobStorageProperties(
        connectionString, corsEnabled, containerName, sharedAccessExpiryTimeSeconds)

@ConfigurationProperties(prefix = "custom.imports-quarantine")
data class QuarantineBlobStorageProperties(
    override val connectionString: String,
    override val corsEnabled: Boolean = false,
    override val containerName: String = "uploads",
    override val sharedAccessExpiryTimeSeconds: Long = 60L
) :
    AbstractBlobStorageProperties(
        connectionString, corsEnabled, containerName, sharedAccessExpiryTimeSeconds)
