/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.repository

import com.bosch.pt.iot.smartsite.common.repository.existencecache.EvictExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.PopulateExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.UseExistenceCache
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantsPerProject
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ParticipantRepository : JpaRepository<Participant, Long>, ParticipantRepositoryExtension {

  fun findOneByIdentifier(identifier: ParticipantId): Participant?

  @Query("select p.id from Participant p where p.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: ParticipantId): Long?

  @PopulateExistenceCache(cacheName = "participant", keyFromResult = ["identifier"])
  fun findAllByIdentifierIn(identifiers: List<ParticipantId?>): List<Participant>

  @UseExistenceCache(cacheName = "participant", keyFromParameters = ["identifier"])
  @Query(
      "select count(p) > 0 from Participant p " +
          "where p.identifier = :identifier " +
          "and p.status = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE")
  fun existsByIdentifierAndParticipantStatusIsActive(
      @Param("identifier") identifier: ParticipantId
  ): Boolean

  @UseExistenceCache(cacheName = "participant", keyFromParameters = ["identifier"])
  @Query(
      "select count(p) > 0 from Participant p " +
          "where p.identifier = :identifier " +
          "and p.project.identifier = :projectIdentifier " +
          "and p.status = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE")
  fun existsByIdentifierAndProjectIdAndParticipantStatusIsActive(
      @Param("identifier") identifier: ParticipantId,
      @Param("projectIdentifier") projectIdentifier: ProjectId
  ): Boolean

  @Query("select p.project.identifier from Participant p where p.identifier = :identifier")
  fun findProjectIdentifierByParticipantIdentifier(
      @Param("identifier") identifier: ParticipantId
  ): ProjectId?

  @EntityGraph(attributePaths = ["project", "company", "user"])
  @Query(
      "select p from Participant p " +
          "where p.identifier = :identifier " +
          "and p.status = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE")
  fun findOneWithProjectAndCompanyAndUserByIdentifierAndActiveTrue(
      @Param("identifier") identifier: ParticipantId
  ): Participant?

  @EntityGraph(
      attributePaths =
          [
              "project",
              "company",
              "user.phonenumbers",
              "user.email",
              "user.crafts",
              "createdBy",
              "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: ParticipantId): Participant?

  fun findOneByUserIdentifierAndProjectIdentifier(
      userIdentifier: UUID,
      projectIdentifier: ProjectId
  ): Participant?

  fun existsByUserIdentifierAndProjectIdentifier(
      userIdentifier: UUID,
      projectIdentifier: ProjectId
  ): Boolean

  fun findOneByUserIdentifierAndCompanyIdentifierAndProjectIdentifier(
      userIdentifier: UUID,
      companyIdentifier: UUID,
      projectIdentifier: ProjectId
  ): Participant?

  @EntityGraph(attributePaths = ["user", "user.profilePicture", "project"])
  @Query(
      "select p from Participant p " +
          "where p.project.identifier in :projectIdentifiers " +
          "and p.role = :role " +
          "and p.status = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE")
  fun findAllByProjectIdentifierInAndRoleAndActiveTrue(
      @Param("projectIdentifiers") projectIdentifiers: Set<ProjectId>,
      @Param("role") role: ParticipantRoleEnum
  ): List<Participant>

  @EntityGraph(
      attributePaths =
          [
              "project",
              "company",
              "user.phonenumbers",
              "user.email",
              "user.crafts",
              "createdBy",
              "lastModifiedBy"])
  @Query(
      "select p from Participant p " +
          "where p.identifier in :identifiers " +
          "and p.status = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE")
  fun findAllWithDetailsByIdentifierInAndActiveTrue(
      @Param("identifiers") identifiers: Collection<ParticipantId>,
      sort: Sort
  ): List<Participant>

  @EntityGraph(
      attributePaths =
          [
              "project",
              "company",
              "user.phonenumbers",
              "user.email",
              "user.crafts",
              "createdBy",
              "lastModifiedBy"])
  @Query(
      "select p from Participant p " +
          "left join p.user " +
          "left join p.company " +
          "inner join p.project " +
          "where p.identifier in :identifiers")
  fun findAllWithDetailsByIdentifierIn(
      @Param("identifiers") identifiers: Collection<ParticipantId>
  ): List<Participant>

  @EntityGraph(attributePaths = ["project", "user"])
  @Query(
      "select p from Participant p " +
          "where p.project.identifier in :projectIdentifiers " +
          "and p.user.identifier in :userIdentifiers " +
          "and p.status = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE")
  fun findAllByProjectIdentifierInAndUserIdentifierInAndActiveTrue(
      @Param("projectIdentifiers") projectIdentifiers: Set<ProjectId>,
      @Param("userIdentifiers") userIdentifiers: Set<UUID>
  ): List<Participant>

  @EntityGraph(attributePaths = ["project", "user", "user.profilePicture"])
  @Query(
      "select p from Participant p " +
          "where p.project.identifier in :projectIdentifiers " +
          "and p.user.identifier in :userIdentifiers")
  fun findAllByProjectIdentifierInAndUserIdentifierIn(
      @Param("projectIdentifiers") projectIdentifiers: Set<ProjectId>,
      @Param("userIdentifiers") userIdentifiers: Set<UUID>
  ): List<Participant>

  @EntityGraph(attributePaths = ["project", "user"])
  @Query(
      "select p from Participant p " +
          "where p.project.identifier = :projectIdentifier " +
          "and p.user.identifier in :userIdentifiers")
  fun findAllByProjectIdentifierAndUserIdentifierIn(
      @Param("projectIdentifier") projectIdentifiers: ProjectId,
      @Param("userIdentifiers") userIdentifiers: Set<UUID>
  ): List<Participant>

  /**
   * Important: Due to DATAJPA-209 we need to use COALESCE for null checking:
   * https://github.com/spring-projects/spring-data-jpa/issues/622
   */
  @Query(
      value =
          "select part.identifier from Participant part " +
              "where part.project.identifier = :projectIdentifier " +
              "and (:companyIdentifier is null or part.company.identifier = :companyIdentifier) " +
              "and part.status = com.bosch.pt.iot.smartsite.project." +
              "participant.shared.model.ParticipantStatusEnum.ACTIVE",
      countQuery =
          "select count (part) from Participant part " +
              "where part.project.identifier = :projectIdentifier " +
              "and (:companyIdentifier is null or part.company.identifier = :companyIdentifier) " +
              "and part.status = com.bosch.pt.iot.smartsite.project.participant." +
              "shared.model.ParticipantStatusEnum.ACTIVE")
  fun findAllParticipantIdentifiersByOptionalCompany(
      @Param("projectIdentifier") projectIdentifier: ProjectId,
      @Param("companyIdentifier") companyIdentifier: UUID?,
      pageable: Pageable
  ): Page<ParticipantId>

  /**
   * Important: Due to DATAJPA-209 we need to use COALESCE for null checking:
   * https://github.com/spring-projects/spring-data-jpa/issues/622
   */
  @Query(
      value =
          "select part.identifier from Participant part " +
              "left join part.user " +
              "left join part.company " +
              "inner join part.project " +
              "where part.project.identifier = :projectIdentifier " +
              "and (:companyIdentifier is null or part.company.identifier = :companyIdentifier) " +
              "and (:searchAllRoles = true or part.role in (:roles)) " +
              "and (:searchAllStatus = true or part.status in (:status))",
      countQuery =
          "select count (part) from Participant part " +
              "left join part.user " +
              "left join part.company " +
              "inner join part.project " +
              "where part.project.identifier = :projectIdentifier " +
              "and (:companyIdentifier is null or part.company.identifier = :companyIdentifier) " +
              "and (:searchAllRoles = true or part.role in (:roles)) " +
              "and (:searchAllStatus = true or part.status in (:status))")
  fun findAllParticipantIdentifiersByFilters(
      @Param("projectIdentifier") projectIdentifier: ProjectId?,
      @Param("searchAllStatus") searchAllStatus: Boolean,
      @Param("status") status: Set<ParticipantStatusEnum>?,
      @Param("companyIdentifier") companyIdentifier: UUID?,
      @Param("searchAllRoles") searchAllRoles: Boolean,
      @Param("roles") roles: Set<ParticipantRoleEnum>?,
      pageable: Pageable
  ): Page<ParticipantId>

  // Hibernate 6 requires that comp.id is in list of result attributes to be able to sort
  @Query(
      value =
          "select distinct comp, comp.id from Participant part join part.company comp " +
              "where part.project.identifier = :projectIdentifier " +
              "and (:includeInactive = true or " +
              "part.status = com.bosch.pt.iot.smartsite.project.participant." +
              "shared.model.ParticipantStatusEnum.ACTIVE) " +
              "order by comp.name asc, comp.id asc",
      countQuery =
          "select count (distinct comp) from Participant part join part.company comp " +
              "where part.project.identifier = :projectIdentifier " +
              "and (:includeInactive = true or " +
              "part.status = com.bosch.pt.iot.smartsite.project.participant." +
              "shared.model.ParticipantStatusEnum.ACTIVE)")
  fun findAllParticipantCompanies(
      @Param("projectIdentifier") projectIdentifier: ProjectId,
      @Param("includeInactive") includeInactive: Boolean,
      pageable: Pageable
  ): Page<Company>

  @Query("select pp from Participant pp join pp.project p where p.id = :id")
  fun findAllByProjectId(@Param("id") projectId: Long): List<Participant>

  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<Participant>

  fun findAllByCompanyIdentifierAndUserIdentifier(
      companyIdentifier: UUID,
      userIdentifier: UUID
  ): List<Participant>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto(" +
          "p.identifier, p.project.identifier, p.user.identifier, p.company.identifier, p.role) " +
          "from Participant p " +
          "where p.project.identifier in :projectIdentifier " +
          "and p.user.identifier = :userIdentifier " +
          "and p.status = com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE")
  fun findByProjectIdentifierInAndUserIdentifierAndActiveTrue(
      @Param("projectIdentifier") projectIdentifier: Set<ProjectId>,
      @Param("userIdentifier") userIdentifier: UUID
  ): Set<ParticipantAuthorizationDto>

  fun findAllByUserIdentifier(userIdentifier: UUID): Set<Participant>

  @Query(
      "select new com.bosch.pt.iot.smartsite.project.participant.shared." +
          "repository.dto.ParticipantsPerProject(p.identifier, count(pp)) " +
          "from Participant pp " +
          "join pp.project p " +
          "where p.identifier in :projectIdentifiers " +
          "and pp.status = com.bosch.pt.iot.smartsite.project.participant." +
          "shared.model.ParticipantStatusEnum.ACTIVE " +
          "group by p.identifier")
  fun countActiveParticipantsPerProject(
      @Param("projectIdentifiers") projectIdentifiers: Set<ProjectId>
  ): Set<ParticipantsPerProject>

  fun countAllByCompany(company: Company): Int

  @Query(
      "select distinct part.user.identifier from Participant part " +
          "where part.project.identifier = :projectIdentifier " +
          "and part.company.identifier in :companyIdentifier " +
          "and part.status = com.bosch.pt.iot.smartsite.project.participant." +
          "shared.model.ParticipantStatusEnum.ACTIVE")
  fun findIdentifiersOfUsersParticipatingInProjectForGivenCompany(
      @Param("projectIdentifier") projectIdentifier: ProjectId,
      @Param("companyIdentifier") companyIdentifier: UUID
  ): Set<UUID>

  @Query(
      "select distinct part from Participant part " +
          "where part.user.identifier = :userIdentifier " +
          "and part.status = com.bosch.pt.iot.smartsite.project.participant." +
          "shared.model.ParticipantStatusEnum.VALIDATION")
  fun findAllInValidationByUserIdentifier(
      @Param("userIdentifier") userIdentifier: UUID
  ): List<Participant>

  // This does not trigger hibernate lifecycle listeners on purpose. The email attribute
  // is a redundant attribute on the participant and does not change the participant version
  // The sub-select is a workaround since joins are not supported in an update statement in JPQL
  @Modifying
  @Query(
      "update Participant p set p.email = null where p.user in " +
          "(select u from User u where u.identifier = :userIdentifier)")
  fun removeEmailFromParticipantsOfUser(@Param("userIdentifier") userIdentifier: UUID)

  // This does not trigger hibernate lifecycle listeners on purpose. The email attribute
  // is a redundant attribute on the participant and does not change the participant version
  // The sub-select is a workaround since joins are not supported in an update statement in JPQL
  @Modifying
  @Query(
      "update Participant p set p.email = :email where p.user in " +
          "(select u from User u where u.identifier = :userIdentifier) and p.email <> :email")
  fun updateEmailOnParticipantsOfUser(
      @Param("userIdentifier") userIdentifier: UUID,
      @Param("email") email: String
  )

  @Query("select part.identifier from Participant part where part.identifier in :identifiers")
  fun validateExistingIdentifiersFor(
      @Param("identifiers") identifiers: Collection<ParticipantId>
  ): Collection<ParticipantId>

  @EvictExistenceCache override fun delete(participant: Participant)

  @EvictExistenceCache override fun deleteAll(participants: MutableIterable<Participant>)
}
