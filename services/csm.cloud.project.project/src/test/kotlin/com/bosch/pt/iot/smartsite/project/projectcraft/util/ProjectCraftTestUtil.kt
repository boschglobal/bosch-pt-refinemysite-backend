/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.util

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFTLIST
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListAggregateAvro
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2.Companion.validateAuditingInformationAndIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2.Companion.validateCreatedAggregateAuditInfoAndAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2.Companion.validateDeletedAggregateAuditInfoAndAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2.Companion.validateUpdatedAggregateAuditInfoAndAggregateIdentifier
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request.SaveProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat

object ProjectCraftTestUtil {

  fun verifyCreatedAggregate(
      aggregate: ProjectCraftAggregateG2Avro,
      resource: SaveProjectCraftResource,
      projectIdentifier: ProjectId,
      testUser: User
  ) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(
            this, ProjectmanagementAggregateTypeEnum.PROJECTCRAFT, testUser)
        assertThat(getProjectIdentifier()).isEqualTo(projectIdentifier.identifier)
        assertThat(name).isEqualTo(resource.name)
        assertThat(color).isEqualTo(resource.color)
      }

  fun verifyUpdatedAggregate(
      aggregate: ProjectCraftAggregateG2Avro,
      projectCraft: ProjectCraft,
      projectIdentifier: ProjectId
  ) =
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, projectCraft, ProjectmanagementAggregateTypeEnum.PROJECTCRAFT)
        assertThat(getProjectIdentifier()).isEqualTo(projectIdentifier.identifier)
        assertThat(name).isEqualTo(projectCraft.name)
        assertThat(color).isEqualTo(projectCraft.color)
      }

  fun verifyDeletedAggregate(
      aggregate: ProjectCraftAggregateG2Avro,
      projectCraft: ProjectCraft,
      projectIdentifier: ProjectId,
      testUser: User
  ) =
      with(aggregate) {
        validateDeletedAggregateAuditInfoAndAggregateIdentifier(
            this, projectCraft, ProjectmanagementAggregateTypeEnum.PROJECTCRAFT, testUser)
        assertThat(getProjectIdentifier()).isEqualTo(projectIdentifier.identifier)
        assertThat(name).isEqualTo(projectCraft.name)
        assertThat(color).isEqualTo(projectCraft.color)
      }

  fun verifyCreatedAggregate(
      aggregate: ProjectCraftListAggregateAvro,
      projectIdentifier: ProjectId,
      testUser: User
  ) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(this, PROJECTCRAFTLIST, testUser)
        assertThat(getProjectIdentifier()).isEqualTo(projectIdentifier.identifier)
        assertThat(projectCrafts).isEmpty()
      }

  fun verifyUpdatedAggregate(
      aggregate: ProjectCraftListAggregateAvro,
      projectIdentifier: ProjectId,
      projectCraftList: ProjectCraftList
  ) =
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, projectCraftList, PROJECTCRAFTLIST)
        assertThat(getProjectIdentifier()).isEqualTo(projectIdentifier.identifier)
        assertThat(projectCrafts)
            .extracting<UUID> { it.identifier.toUUID() }
            .containsExactlyElementsOf(
                projectCraftList.projectCrafts.map { it.identifier.identifier })
      }

  fun validateProjectCraft(
      projectCraft: ProjectCraft,
      aggregate: ProjectCraftAggregateG2Avro,
      projectIdentifier: ProjectId
  ) =
      with(projectCraft) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(color).isEqualTo(aggregate.color)
        assertThat(name).isEqualTo(aggregate.name)
      }

  fun validateProjectCraftList(
      projectCraftList: ProjectCraftList,
      aggregate: ProjectCraftListAggregateAvro,
      projectIdentifier: ProjectId,
      projectCraftIdentifiers: List<ProjectCraftId> = listOf()
  ) =
      with(projectCraftList) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(projectCraftList.projectCrafts)
            .extracting<ProjectCraftId>(ProjectCraft::identifier)
            .containsExactlyElementsOf(projectCraftIdentifiers)
      }
}
