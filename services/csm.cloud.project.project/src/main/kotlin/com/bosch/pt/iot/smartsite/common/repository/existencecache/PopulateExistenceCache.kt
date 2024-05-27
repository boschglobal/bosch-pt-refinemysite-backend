/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository.existencecache

/**
 * The use of this annotation will populate the ExistenceCache with a reference between the given
 * cache name and key and the identifier of the found entities.
 */
annotation class PopulateExistenceCache(

    /**
     * a string that will be used as cache name, making possible to create multiple different caches
     * for the same entity as is easier to identify the cache that is being used.
     */
    val cacheName: String,

    /**
     * a Spring expression language (SpEl) expression pointing to the field of the annotated
     * method's return value that will be used as cache key, e.g. "project.identifier".
     */
    val keyFromResult: Array<String> = []
)
