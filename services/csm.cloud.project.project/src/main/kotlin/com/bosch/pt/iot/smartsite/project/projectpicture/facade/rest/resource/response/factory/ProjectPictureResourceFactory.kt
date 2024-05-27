/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.LinkUtils.linkFromUriComponents
import com.bosch.pt.iot.smartsite.common.facade.rest.LinkUtils.linkTemplateWithPathSegments
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectpicture.authorization.ProjectPictureAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PATH_VARIABLE_PICTURE_ID
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.ProjectPictureResource
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.ProjectPictureResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.ProjectPictureResource.Companion.LINK_FULL
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response.ProjectPictureResource.Companion.LINK_SMALL
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import java.util.Locale
import org.springframework.context.MessageSource
import org.springframework.hateoas.Link
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentServletMapping

@Component
class ProjectPictureResourceFactory(
    messageSource: MessageSource,
    private val userService: UserService,
    private val projectPictureAuthorizationComponent: ProjectPictureAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory
) : AbstractResourceFactoryHelper(messageSource) {

  @JvmOverloads
  fun build(
      projectPicture: ProjectPicture,
      hasDeletePermissionOnPicture: Boolean =
          projectPictureAuthorizationComponent.hasDeletePermissionOnPicture(
              projectPicture.identifier!!)
  ): ProjectPictureResource {

    val linkTemplate =
        fromCurrentServletMapping()
            .path(getCurrentApiVersionPrefix())
            .path("/projects/{projectId}/picture/{projectPictureId}/{size}")

    return ProjectPictureResource(
            id = projectPicture.identifier!!,
            version = projectPicture.version!!,
            createdDate = projectPicture.createdDate.get().toDate(),
            createdBy = referTo(projectPicture.createdBy, deletedUserReference)!!,
            lastModifiedDate = projectPicture.lastModifiedDate.get().toDate(),
            lastModifiedBy = referTo(projectPicture.lastModifiedBy, deletedUserReference)!!,
            width = projectPicture.width!!,
            height = projectPicture.height!!,
            fileSize = projectPicture.fileSize!!,
            projectReference = ResourceReference.from(projectPicture.project!!))
        .apply {
          add(
              linkFromUriComponents(
                  linkTemplate.buildAndExpand(
                      projectPicture.project!!.identifier,
                      projectPicture.identifier,
                      SMALL.name.lowercase(Locale.getDefault())),
                  LINK_SMALL))

          add(
              linkFromUriComponents(
                  linkTemplate.buildAndExpand(
                      projectPicture.project!!.identifier,
                      projectPicture.identifier,
                      FULL.name.lowercase(Locale.getDefault())),
                  LINK_FULL))

          // add link to delete the project picture
          addIf(hasDeletePermissionOnPicture) {
            linkFactory
                .linkTo(PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT)
                .withParameters(
                    mapOf(
                        PATH_VARIABLE_PROJECT_ID to projectPicture.project!!.identifier,
                        PATH_VARIABLE_PICTURE_ID to projectPicture.identifier!!))
                .withRel(LINK_DELETE)
          }
        }
  }

  fun buildDefault(project: Project): ProjectPictureResource {
    val projectPictureLink =
        linkTemplateWithPathSegments(FILE_NAME_DEFAULT_PROJECT_PICTURE).build().toUri()

    val auditingUser = userService.findAuditUsers(setOf(project))

    return ProjectPictureResource(
            id = project.identifier.toUuid(),
            version = project.version,
            createdDate = project.createdDate.get().toDate(),
            createdBy = referTo(auditingUser[project.createdBy.get()]!!, deletedUserReference)!!,
            lastModifiedDate = project.lastModifiedDate.get().toDate(),
            lastModifiedBy =
                referTo(auditingUser[project.lastModifiedBy.get()]!!, deletedUserReference)!!,
            projectReference = ResourceReference.from(project))
        .apply {
          add(Link.of(projectPictureLink.toString(), LINK_SMALL))
          add(Link.of(projectPictureLink.toString(), LINK_FULL))
        }
  }

  companion object {
    private const val FILE_NAME_DEFAULT_PROJECT_PICTURE = "default-project-picture.png"
  }
}
