/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.repository

import com.bosch.pt.csm.cloud.projectmanagement.news.model.News
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NewsRepository : JpaRepository<News, Long>, NewsRepositoryExtension {

  @Query("select n.id from News n where n.rootObject = :identifier")
  fun findIdsByRootObject(@Param("identifier") objectIdentifier: ObjectIdentifier): List<Long>

  @Query("select n.id from News n where n.contextObject = :identifier")
  fun findIdsByContextObject(@Param("identifier") objectIdentifier: ObjectIdentifier): List<Long>

  @Query("select n.id from News n where n.parentObject = :identifier")
  fun findIdsByParentObject(@Param("identifier") objectIdentifier: ObjectIdentifier): List<Long>

  fun findByContextObjectIn(contextObject: Collection<ObjectIdentifier>): List<News>

  fun findAllByUserIdentifierAndRootObject(
      userIdentifier: UUID,
      rootObject: ObjectIdentifier
  ): List<News>

  fun findAllByUserIdentifierAndContextObjectIn(
      userIdentifier: UUID,
      contextObjects: List<ObjectIdentifier>
  ): List<News>

  fun deleteAllByUserIdentifierAndRootObject(userIdentifier: UUID, rootObject: ObjectIdentifier)

  fun deleteAllByUserIdentifierAndRootObjectIn(
      userIdentifier: UUID,
      rootObjects: List<ObjectIdentifier>
  )

  fun deleteAllByUserIdentifier(userIdentifier: UUID)
}
