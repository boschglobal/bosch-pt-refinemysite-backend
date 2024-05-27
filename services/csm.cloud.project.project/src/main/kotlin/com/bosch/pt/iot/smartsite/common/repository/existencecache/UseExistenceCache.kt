/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository.existencecache

/**
 * The use of this annotation will check the ExistenceCache for an entry for the given cache name
 * and keys from the function parameters. If such an entry is found, the annotated method will
 * return true without querying the database.
 */
annotation class UseExistenceCache(

    /** a string that indicates the name of the cache that will be checked. */
    val cacheName: String,

    /**
     * a array of strings referring the function parameters that will be used as keys to check the
     * cache for the existence of the entity.
     */
    val keyFromParameters: Array<String> = []
)
