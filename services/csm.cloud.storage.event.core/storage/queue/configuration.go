package queue

import (
	"csm.cloud.storage.event.core/config/properties"
	"csm.cloud.storage.event.core/storage/messages/delete"
	"csm.cloud.storage.event.core/storage/messages/get"
)

type Configuration struct {
	getMessagesService   get.GetMessagesService
	getBlobInfoService   get.GetBlobInfoService
	deleteMessageService delete.DeleteMessageService
	storageConfig        properties.StorageProperties
}

func NewDefaultConfiguration(storageConfig properties.StorageProperties) Configuration {
	return Configuration{
		getMessagesService:   get.NewDefaultGetMessageService(storageConfig),
		getBlobInfoService:   get.NewDefaultGetBlobInfoService(storageConfig),
		deleteMessageService: delete.NewDefaultDeleteMessageService(storageConfig),
		storageConfig:        storageConfig,
	}
}
