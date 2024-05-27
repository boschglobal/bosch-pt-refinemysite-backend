package image

import (
	"context"
	"csm.cloud.image.scale/domain"
	"csm.cloud.image.scale/image/model"
	"csm.cloud.image.scale/kafka/producer"
	"csm.cloud.image.scale/storage"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/datadog"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/retry"
	"errors"
	"fmt"
	"github.com/google/uuid"
	"github.com/rs/zerolog/log"
	"golang.org/x/text/cases"
	"golang.org/x/text/language"
	"path"
	"regexp"
	"strings"
	"time"
)

const contentTypeJpeg = "image/jpeg"

type ImageScalingProcessor struct {
	quarantineBlobStorageClient storage.BlobStorageClient
	projectBlobStorageClient    storage.BlobStorageClient
	userBlobStorageClient       storage.BlobStorageClient
	imageDeletedEventProducer   producer.EventKafkaProducer[domain.MessageKey, domain.ImageDeletedEvent]
	imageScaledEventProducer    producer.EventKafkaProducer[domain.MessageKey, domain.ImageScaledEvent]
}

func NewImageScalingProcessor(quarantineBlobStorageClient storage.BlobStorageClient,
	projectBlobStorageClient storage.BlobStorageClient,
	userBlobStorageClient storage.BlobStorageClient,
	imageDeletedEventProducer producer.EventKafkaProducer[domain.MessageKey, domain.ImageDeletedEvent],
	imageScaledEventProducer producer.EventKafkaProducer[domain.MessageKey, domain.ImageScaledEvent],
) ImageScalingProcessor {
	return ImageScalingProcessor{
		quarantineBlobStorageClient: quarantineBlobStorageClient,
		projectBlobStorageClient:    projectBlobStorageClient,
		userBlobStorageClient:       userBlobStorageClient,
		imageDeletedEventProducer:   imageDeletedEventProducer,
		imageScaledEventProducer:    imageScaledEventProducer,
	}
}

func (i *ImageScalingProcessor) ScaleImageWithRetry(tracingContext context.Context, event domain.FileCreatedEvent) error {
	key, err := i.scaleWithRetry(tracingContext, event)
	if err != nil {
		// If the image couldn't be scaled repetitive, delete it
		log.Warn().Msg(fmt.Sprintf("Deleting image that couldn't be scaled repetitive %s/%s for reason: %s", event.Path, event.FileName, err))
		deleteErr := i.deleteImageFromQuarantineBlobStorage(tracingContext, event)
		if deleteErr != nil {
			log.Error().Msg(fmt.Sprintf("File couldn't be delete from quarantine blob storage repetitive %s/%s", event.Path, event.FileName))
		}
		if key != nil {
			// Send kafka message to inform downstream services
			return i.sendImageDeletedEvent(tracingContext, *key, event)
		}
	}

	return nil
}

func (i *ImageScalingProcessor) scaleWithRetry(tracingContext context.Context, event domain.FileCreatedEvent) (*domain.MessageKey, error) {
	var key *domain.MessageKey
	err := retry.SimpleRetryWithTracingContext(func(tracingContext context.Context) error {
		// Download image
		log.Info().Msg(fmt.Sprintf("Download image: %s", event.FileName))
		blob, err := i.downloadImage(tracingContext, event)
		if err != nil {
			return err
		}
		timezone := i.getTimezone(blob)

		// Extract image metadata from event parameters
		imageMetadata, err := i.getImageMetadata(blob, event.Path, event.FileName, event.Identifier, event.ContentType)
		if err != nil {
			return err
		}
		var image = imageMetadata.(model.Image)
		caser := cases.Title(language.English)
		objectType := strings.Replace(caser.String(strings.ToLower(strings.Replace(image.GetOwnerType(), "_", " ", -1))), " ", "", -1)

		if image.GetBoundedContext() == model.PROJECT {
			log.Info().Msg(fmt.Sprintf("Scale %s: %s", objectType, event.FileName))
			original, fullSize, small, err := i.scaleProjectPicture(tracingContext, blob, objectType)
			if err != nil {
				return err
			}
			log.Info().Msg(fmt.Sprintf("Upload %s: %s", objectType, event.FileName))
			err = i.uploadProjectPicture(tracingContext, image, original, fullSize, small, objectType, *timezone)
			if err != nil {
				return err
			}
		} else if image.GetBoundedContext() == model.USER {
			log.Info().Msg(fmt.Sprintf("Scale %s: %s", objectType, event.FileName))
			original, small, err := i.scaleUserPicture(tracingContext, blob, objectType)
			if err != nil {
				return err
			}
			log.Info().Msg(fmt.Sprintf("Upload %s: %s", objectType, event.FileName))
			err = i.uploadUserPicture(tracingContext, image, original, small, objectType, *timezone)
			if err != nil {
				return err
			}
		} else {
			return fmt.Errorf("image with invalid path detected %s/%s", event.Path, event.FileName)
		}

		log.Info().Msg(fmt.Sprintf("Send kafka event for %s: %s", objectType, event.FileName))
		key = i.getMessageKey(imageMetadata)
		err = i.sendImageScaledEvent(tracingContext, *key, event)
		if err != nil {
			return err
		}
		log.Info().Msg(fmt.Sprintf("Delete %s from quarantine blob storage: %s", objectType, event.FileName))
		return i.deleteImageFromQuarantineBlobStorage(tracingContext, event)
	}, tracingContext, 5, 1*time.Second, "ScaleWithRetry")

	return key, err
}

func (i *ImageScalingProcessor) getTimezone(blob *storage.Blob) *string {
	timezone := blob.Metadata["timezone"]
	if timezone == nil {
		utc := "UTC"
		return &utc
	}
	return timezone
}

func (i *ImageScalingProcessor) downloadImage(tracingContext context.Context, event domain.FileCreatedEvent) (*storage.Blob, error) {
	blob, err := datadog.TraceWithContext(tracingContext, "downloadImage", func() (*storage.Blob, error) {
		return i.quarantineBlobStorageClient.DownloadBlob(event.Path, event.FileName)
	})
	if err != nil {
		return nil, err
	}
	return blob, nil
}

func (i ImageScalingProcessor) getMessageKey(imageMetadata interface{}) *domain.MessageKey {
	var image = imageMetadata.(model.Image)
	return &domain.MessageKey{
		RootContextIdentifier: image.GetRootContextIdentifier(),
		AggregateIdentifier: domain.AggregateIdentifier{
			Identifier: image.GetOwnerIdentifier(),
			Version:    0,
			Type:       image.GetAggregateType(),
		},
	}
}

func (i ImageScalingProcessor) scaleProjectPicture(tracingContext context.Context, blob *storage.Blob, objectType string) (*[]byte, *[]byte, *[]byte, error) {

	// Scale full image
	fullImage, err := datadog.TraceWithContext(tracingContext, fmt.Sprintf("scale%sFull", objectType), func() (*[]byte, error) {
		return ScaleImage(&blob.Buffer, &PreviewImageSizeProperties)
	})
	if err != nil {
		return nil, nil, nil, err
	}

	// Scale small image
	smallImage, err := datadog.TraceWithContext(tracingContext, fmt.Sprintf("scale%sSmall", objectType), func() (*[]byte, error) {
		return ScaleImage(&blob.Buffer, &SmallImageSizeProperties)
	})
	if err != nil {
		return nil, nil, nil, err
	}

	return &blob.Buffer, fullImage, smallImage, nil
}

func (i ImageScalingProcessor) scaleUserPicture(tracingContext context.Context, blob *storage.Blob, objectType string) (*[]byte, *[]byte, error) {

	// Scale small image
	smallImage, err := datadog.TraceWithContext(tracingContext, fmt.Sprintf("scale%sSmall", objectType), func() (*[]byte, error) {
		return ScaleImage(&blob.Buffer, &SmallImageSizeProperties)
	})
	if err != nil {
		return nil, nil, err
	}

	return &blob.Buffer, smallImage, nil
}

func (i ImageScalingProcessor) uploadProjectPicture(tracingContext context.Context, image model.Image, originalImage *[]byte, fullSizeImage *[]byte, smallImage *[]byte, objectType string, timezone string) error {
	fileName := image.GetFileName()
	jpgFileName := i.fileNameAsJpg(fileName)
	ownerIdentifier := image.GetOwnerIdentifier()
	ownerType := image.GetOwnerType()

	metadata := make(map[string]*string)
	metadata["filename"] = &jpgFileName
	metadata["timezone"] = &timezone
	metadata["owner_identifier"] = &ownerIdentifier
	metadata["owner_type"] = &ownerType

	// Upload small image
	_, err := datadog.TraceWithContext(tracingContext, fmt.Sprintf("upload%sSmall", objectType), func() (any, error) {
		err := i.projectBlobStorageClient.UploadBlob(fmt.Sprintf("project/image/small/%s", image.GetParentIdentifier()), ownerIdentifier, smallImage, metadata, contentTypeJpeg)
		return nil, err
	})
	if err != nil {
		return err
	}

	// Upload full image
	_, err = datadog.TraceWithContext(tracingContext, fmt.Sprintf("upload%sFull", objectType), func() (any, error) {
		err := i.projectBlobStorageClient.UploadBlob(fmt.Sprintf("project/image/fullhd/%s", image.GetParentIdentifier()), ownerIdentifier, fullSizeImage, metadata, contentTypeJpeg)
		return nil, err
	})
	if err != nil {
		return err
	}

	// Upload original image
	metadata["filename"] = &fileName
	_, err = datadog.TraceWithContext(tracingContext, fmt.Sprintf("upload%s", objectType), func() (any, error) {
		err := i.projectBlobStorageClient.UploadBlob(fmt.Sprintf("project/image/original/%s", image.GetParentIdentifier()), ownerIdentifier, originalImage, metadata, image.GetContentType())
		return nil, err
	})

	return err
}

func (i ImageScalingProcessor) uploadUserPicture(tracingContext context.Context, image model.Image, originalImage *[]byte, smallImage *[]byte, objectType string, timezone string) error {
	fileName := image.GetFileName()
	jpgFileName := i.fileNameAsJpg(fileName)
	ownerIdentifier := image.GetOwnerIdentifier()
	ownerType := image.GetOwnerType()

	metadata := make(map[string]*string)
	metadata["filename"] = &jpgFileName
	metadata["timezone"] = &timezone
	metadata["owner_identifier"] = &ownerIdentifier
	metadata["owner_type"] = &ownerType

	// Upload small image
	_, err := datadog.TraceWithContext(tracingContext, fmt.Sprintf("upload%sSmall", objectType), func() (any, error) {
		err := i.userBlobStorageClient.UploadBlob(fmt.Sprintf("user/image/small/%s", image.GetParentIdentifier()), ownerIdentifier, smallImage, metadata, contentTypeJpeg)
		return nil, err
	})
	if err != nil {
		return err
	}

	// Upload original image
	metadata["filename"] = &fileName
	_, err = datadog.TraceWithContext(tracingContext, fmt.Sprintf("upload%s", objectType), func() (any, error) {
		err := i.userBlobStorageClient.UploadBlob(fmt.Sprintf("user/image/original/%s", image.GetParentIdentifier()), ownerIdentifier, originalImage, metadata, image.GetContentType())
		return nil, err
	})

	return err
}

func (i *ImageScalingProcessor) deleteImageFromQuarantineBlobStorage(tracingContext context.Context, event domain.FileCreatedEvent) error {
	_, err := datadog.TraceWithContext(tracingContext, "deleteImage", func() (any, error) {
		return nil, i.quarantineBlobStorageClient.DeleteBlob(event.Path, event.FileName)
	})
	return err
}

func (i *ImageScalingProcessor) fileNameAsJpg(fileName string) string {
	fileExtension := path.Ext(fileName)
	return fileName[0:len(fileName)-len(fileExtension)] + ".jpg"
}

func (i *ImageScalingProcessor) sendImageScaledEvent(tracingContext context.Context, key domain.MessageKey, event domain.FileCreatedEvent) error {
	_, err := datadog.TraceWithContext(tracingContext, "sendImageScaledEvent", func() (any, error) {
		imageScaledEvent := domain.ImageScaledEvent(event)
		err := i.imageScaledEventProducer.Produce(tracingContext, key, imageScaledEvent)
		return nil, err
	})
	return err
}

func (i *ImageScalingProcessor) sendImageDeletedEvent(tracingContext context.Context, key domain.MessageKey, event domain.FileCreatedEvent) error {
	_, err := datadog.TraceWithContext(tracingContext, "sendImageDeletedEvent", func() (any, error) {
		imageDeletedEvent := domain.ImageDeletedEvent(event)
		err := i.imageDeletedEventProducer.Produce(tracingContext, key, imageDeletedEvent)
		return nil, err
	})
	return err
}

func (i *ImageScalingProcessor) getImageMetadata(blob *storage.Blob, path string, eventFileName string, eventId string, eventContentType string) (interface{}, error) {
	// Get metadata from blob metadata. If a file is uploaded with the azure storage explorer
	// to test the converter, no metadata is set. Therefore, data is taken from event parameters or random generated.
	fileName, blobHasFileName := blob.Metadata["filename"]
	if !blobHasFileName {
		fileName = &eventFileName
	}

	contentType := blob.ContentType
	if contentType == nil {
		contentType = &eventContentType
	}

	// Chose owner identifier from blob-metadata, event-id, event-file-name or random-UUID
	ownerIdentifier, blobHasOwnerIdentifier := blob.Metadata["owner_identifier"]
	if !blobHasOwnerIdentifier {
		// Take event-id if it is a valid UUID
		_, err := uuid.Parse(eventId)
		if err == nil {
			ownerIdentifier = &eventId
		} else {
			// Take event-file-name if it is a valid UUID
			_, err = uuid.Parse(eventFileName)
			if err == nil {
				ownerIdentifier = &eventFileName
			} else {
				// Take random UUID
				randomUuid, err := uuid.NewUUID()
				if err != nil {
					return nil, err
				}
				randomUuidString := randomUuid.String()
				ownerIdentifier = &randomUuidString
			}
		}
	}

	// project picture
	pattern := regexp.MustCompile("/images/projects/(?P<projectIdentifier>[^/]*)/picture")
	matches := pattern.FindAllStringSubmatch(path, -1)

	if matches != nil {
		return &model.ProjectPicture{
			FileName:                 *fileName,
			ProjectIdentifier:        matches[0][1],
			ProjectPictureIdentifier: *ownerIdentifier,
			ContentType:              *contentType,
		}, nil
	}

	// message attachment
	pattern = regexp.MustCompile("/images/projects/(?P<projectIdentifier>[^/]*)/tasks/(?P<taskIdentifier>[^/]*)/topics/(?P<topicIdentifier>[^/]*)/messages/(?P<messageIdentifier>[^/]*)")
	matches = pattern.FindAllStringSubmatch(path, -1)

	if matches != nil {
		return &model.MessageAttachment{
			FileName:                    *fileName,
			ProjectIdentifier:           matches[0][1],
			TaskIdentifier:              matches[0][2],
			TopicIdentifier:             matches[0][3],
			MessageIdentifier:           matches[0][4],
			MessageAttachmentIdentifier: *ownerIdentifier,
			ContentType:                 *contentType,
		}, nil
	}

	// topic attachment
	pattern = regexp.MustCompile("/images/projects/(?P<projectIdentifier>[^/]*)/tasks/(?P<taskIdentifier>[^/]*)/topics/(?P<topicIdentifier>[^/]*)")
	matches = pattern.FindAllStringSubmatch(path, -1)

	if matches != nil {
		return &model.TopicAttachment{
			FileName:                  *fileName,
			ProjectIdentifier:         matches[0][1],
			TaskIdentifier:            matches[0][2],
			TopicIdentifier:           matches[0][3],
			TopicAttachmentIdentifier: *ownerIdentifier,
			ContentType:               *contentType,
		}, nil
	}

	// task attachment
	pattern = regexp.MustCompile("/images/projects/(?P<projectIdentifier>[^/]*)/tasks/(?P<taskIdentifier>[^/]*)")
	matches = pattern.FindAllStringSubmatch(path, -1)

	if matches != nil {
		return &model.TaskAttachment{
			FileName:                 *fileName,
			ProjectIdentifier:        matches[0][1],
			TaskIdentifier:           matches[0][2],
			TaskAttachmentIdentifier: *ownerIdentifier,
			ContentType:              *contentType,
		}, nil
	}

	// profile picture
	pattern = regexp.MustCompile("/images/users/(?P<userIdentifier>[^/]*)/picture")
	matches = pattern.FindAllStringSubmatch(path, -1)

	if matches != nil {
		return &model.ProfilePicture{
			FileName:                 *fileName,
			UserIdentifier:           matches[0][1],
			ProfilePictureIdentifier: *ownerIdentifier,
			ContentType:              *contentType,
		}, nil
	}

	return nil, errors.New("unsupported image type")
}
