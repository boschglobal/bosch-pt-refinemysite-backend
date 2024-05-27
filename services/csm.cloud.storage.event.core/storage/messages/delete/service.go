package delete

import (
	"context"
	"csm.cloud.storage.event.core/config/properties"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azqueue"
)

// DeleteMessageService is an interface abstraction for the Delete method of storage.Message
type DeleteMessageService interface {
	DeleteMessage(ctx context.Context, messageID string, popReceipt string, o *azqueue.DeleteMessageOptions) (azqueue.DeleteMessageResponse, error)
}

/*
NewDefaultDeleteMessageService returns a default implementation of DeleteMessageService interface
which uses the Azure Storage Queue configured. As the AzureStorage.Queue is a
suitable implementation no internal implementation is required
*/
func NewDefaultDeleteMessageService(storageConfig properties.StorageProperties) DeleteMessageService {
	client, err := azqueue.NewServiceClientFromConnectionString(storageConfig.ConnectionString, nil)
	if err != nil {
		panic(app.NewFatalError("Connection to AzureStorage failed", err))
	}

	return client.NewQueueClient(storageConfig.QueueName)
}
