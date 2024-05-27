/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.repository

import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier

interface NewsRepositoryExtension {

  fun findIdsPartitioned(taskIdentifiers: List<ObjectIdentifier>): List<Long>

  fun deletePartitioned(ids: List<Long>)
}
