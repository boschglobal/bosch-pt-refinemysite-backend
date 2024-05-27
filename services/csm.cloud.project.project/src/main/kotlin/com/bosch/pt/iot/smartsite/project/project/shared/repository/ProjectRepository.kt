/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.repository

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.common.repository.existencecache.EvictExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.PopulateExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.UseExistenceCache
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.DateByProjectIdentifierDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectRepository :
    JpaRepository<Project, Long>, ProjectRepositoryExtension, JpaSpecificationExecutor<Project> {

  @PopulateExistenceCache(cacheName = "project", keyFromResult = ["identifier"])
  fun findAllByIdentifierIn(identifiers: List<ProjectId>): List<Project>

  @EntityGraph(attributePaths = ["participants"])
  @Query("select p from Project p where p.deleted = false order by p.title asc")
  fun findAllWithDetails(): List<Project>

  @UseExistenceCache(cacheName = "project", keyFromParameters = ["identifier"])
  fun existsByIdentifier(projectIdentifier: ProjectId): Boolean

  @Query(
      "select p from Project p " +
          "join p.participants participant " +
          "join participant.user u " +
          "where p.deleted = false " +
          "and participant.status = " +
          "com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE " +
          "and u.id = ?#{principal?.id}")
  fun findAllWhereCurrentUserIsActiveParticipant(pageable: Pageable): Page<Project>

  fun findOneByIdentifier(identifier: ProjectId): Project?

  @Query("select p.id from Project p where p.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: ProjectId): Long?

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.project.shared.model.dto.DateByProjectIdentifierDto(" +
          "p.identifier, min(pp.createdDate)) " +
          "from Participant pp " +
          "join pp.project p " +
          "where p.identifier in :projectIdentifiers " +
          "and pp.role = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM " +
          "group by p.identifier")
  fun findOldestCsmCreatedDatesByProjectIdentifiers(
      @Param("projectIdentifiers") projectIdentifiers: Collection<ProjectId>
  ): Set<DateByProjectIdentifierDto>

  @Query(
      "select p " +
        "from Project p " +
        "join p.participants pp " +
        "join pp.user u " +
        "join pp.company c " +
        "where cast(pp.id as long) = (select min(cast(ppc.id as long)) from Participant ppc where ppc.project = p) " +
        "and pp.user = u " +
        "and pp.company = c " +
        "and  (:title is null or :title like '' " +
        "or upper(p.title) like upper(concat('%', :title, '%'))) " +
        "and (:creator is null or :creator like '' " +
        "or upper(concat(u.firstName, ' ', u.lastName)) like upper(concat('%', :creator, '%'))) " +
        "and (:company is null or :company like '' " +
        "or upper(c.name) like upper(concat('%', :company, '%'))) " +
        "and (:#{#restrictedCountries.size()} = 0 or u.country in :restrictedCountries) " +
        "and p.deleted = false")
  fun findProjectsForFilters(
      @Param("title") title: String?,
      @Param("company") company: String?,
      @Param("creator") creator: String?,
      @Param("restrictedCountries") restrictedCountries: Set<IsoCountryCodeEnum>,
      pageable: Pageable
  ): Page<Project>

  @EvictExistenceCache override fun delete(project: Project)

  @EvictExistenceCache override fun deleteAll(projects: MutableIterable<Project>)
}
