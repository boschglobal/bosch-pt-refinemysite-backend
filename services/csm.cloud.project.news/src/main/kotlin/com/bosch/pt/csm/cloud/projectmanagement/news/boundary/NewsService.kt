/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.boundary

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.news.model.News
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.news.repository.NewsRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NewsService(
    private val objectRelationService: ObjectRelationService,
    private val newsRepository: NewsRepository
) {

  @Trace
  fun updateNews(newsList: List<News>, recipients: Set<UUID>) {
    val newsToSave = mutableListOf<News>()
    val newsToDelete = mutableListOf<News>()

    // Find existing news for all users
    val existingNewsByContextObject =
        newsRepository.findByContextObjectIn(newsList.map { it.contextObject }).groupBy {
          it.contextObject
        }

    // Map news by their context objects
    newsList.forEach { news ->
      val unprocessedUsers = recipients.toMutableList()

      // Iterate over existing news entries and update entries of users who are marked as recipients
      (existingNewsByContextObject[news.contextObject] ?: emptyList()).forEach { existingNews ->
        // update existing news
        if (unprocessedUsers.contains(existingNews.userIdentifier)) {
          existingNews.lastModifiedDate = news.lastModifiedDate
          newsToSave.add(existingNews)
          unprocessedUsers.remove(existingNews.userIdentifier)
          // delete news for recipients that are no longer valid
        } else {
          newsToDelete.add(existingNews)
        }
      }

      // Collect news for all users who are not having a news for the corresponding context
      // object yet
      newsToSave.addAll(
          unprocessedUsers.map { user: UUID ->
            News(
                news.rootObject,
                news.parentObject,
                news.contextObject,
                user,
                news.createdDate,
                news.lastModifiedDate)
          })
    }
    newsRepository.saveAll(newsToSave)
    newsRepository.deleteAll(newsToDelete)
  }

  @Trace
  @Transactional(readOnly = true)
  fun findAllByUserIdentifierAndRootObject(
      userIdentifier: UUID,
      rootObject: ObjectIdentifier
  ): List<News> = newsRepository.findAllByUserIdentifierAndRootObject(userIdentifier, rootObject)

  @Trace
  @Transactional(readOnly = true)
  fun findAllByUserIdentifierAndContextObjectsIn(
      userIdentifier: UUID,
      contextObjects: List<ObjectIdentifier>
  ): List<News> =
      newsRepository.findAllByUserIdentifierAndContextObjectIn(userIdentifier, contextObjects)

  @Trace
  @Transactional
  fun deleteAllByUserIdentifierAndProjectIdentifier(userIdentifier: UUID, projectIdentifier: UUID) {
    val taskObjectIdentifiers = objectRelationService.findTaskIdentifiers(projectIdentifier)
    deleteAllByUserIdentifierAndRootObjectIn(userIdentifier, taskObjectIdentifiers)
  }

  @Trace
  @Transactional
  fun deleteAllByUserIdentifierAndRootObject(userIdentifier: UUID, rootObject: ObjectIdentifier) =
      newsRepository.deleteAllByUserIdentifierAndRootObject(userIdentifier, rootObject)

  @Trace
  fun deleteAllByUserIdentifierAndRootObjectIn(
      userIdentifier: UUID,
      rootObjects: List<ObjectIdentifier>
  ) = newsRepository.deleteAllByUserIdentifierAndRootObjectIn(userIdentifier, rootObjects)

  @Trace
  fun deleteByTaskIdentifier(taskIdentifier: AggregateIdentifierAvro) =
      newsRepository.deletePartitioned(
          newsRepository.findIdsByRootObject(ObjectIdentifier(taskIdentifier)))

  @Trace
  fun deleteByMessageIdentifier(messageIdentifier: AggregateIdentifierAvro) =
      newsRepository.deletePartitioned(
          ObjectIdentifier(messageIdentifier).let { objectIdentifier ->
            val newsIds = newsRepository.findIdsByContextObject(objectIdentifier).toMutableList()
            newsIds.addAll(newsRepository.findIdsByParentObject(objectIdentifier))
            newsIds
          })

  @Trace
  fun deleteByTaskIdentifiers(taskIdentifiers: List<ObjectIdentifier>) =
      newsRepository.deletePartitioned(newsRepository.findIdsPartitioned(taskIdentifiers))

  @Trace
  fun deleteByUserIdentifier(userIdentifier: UUID) =
      newsRepository.deleteAllByUserIdentifier(userIdentifier)
}
