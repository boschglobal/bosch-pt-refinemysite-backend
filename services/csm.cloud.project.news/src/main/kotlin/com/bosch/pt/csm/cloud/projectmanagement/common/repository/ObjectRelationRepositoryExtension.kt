/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.common.repository

interface ObjectRelationRepositoryExtension {

  fun deletePartitioned(ids: List<Long>)
}
