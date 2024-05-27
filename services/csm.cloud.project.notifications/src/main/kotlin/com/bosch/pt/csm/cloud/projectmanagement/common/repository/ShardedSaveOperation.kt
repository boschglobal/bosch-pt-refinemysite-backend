/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.repository

interface ShardedSaveOperation<T, I> {
    fun <S : T> save(entity: S): S
}
