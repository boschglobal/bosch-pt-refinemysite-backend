package get

import (
	"context"
	"csm.cloud.storage.event.core/config/properties"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azblob"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azblob/service"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azqueue"
	"time"
)

/*
GetMessagesService - Interface abstraction for AzureStorage.getMessagesService
*/
type GetMessagesService interface {
	DequeueMessages(ctx context.Context, o *azqueue.DequeueMessagesOptions) (azqueue.DequeueMessagesResponse, error)
}

/*
NewDefaultGetMessageService returns a default implementation of GetMessagesService interface
which uses the Azure Storage Queue configured. As the AzureStorage.Queue is a
suitable implementation no internal implementation is required
*/
func NewDefaultGetMessageService(storageConfig properties.StorageProperties) GetMessagesService {

	// Connect to Azure Storage
	client, err := azqueue.NewServiceClientFromConnectionString(storageConfig.ConnectionString, nil)
	if err != nil {
		panic(app.NewFatalError("Connection to AzureStorage failed", err))
	}

	return client.NewQueueClient(storageConfig.QueueName)
}

type GetBlobInfoService interface {
	GetBlobProperties(container string, blob string) (*BlobProperties, error)
}

type defaultBlobInfoService struct {
	serviceClient *service.Client
}

type BlobProperties struct {
	ContentLength int64
	ContentType   string
	TraceHeader   *string
}

func (this *defaultBlobInfoService) GetBlobProperties(container string, blob string) (*BlobProperties, error) {
	requestContext, cancelFn := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancelFn()
	getProperties, err := this.serviceClient.NewContainerClient(container).NewBlobClient(blob).GetProperties(requestContext, nil)
	if err != nil {
		return nil, err
	}
	blobProperties := BlobProperties{ContentLength: *getProperties.ContentLength, ContentType: *getProperties.ContentType, TraceHeader: getProperties.Metadata["Trace_header"]}
	return &blobProperties, nil
}

func NewDefaultGetBlobInfoService(storageConfig properties.StorageProperties) GetBlobInfoService {

	client, err := azblob.NewClientFromConnectionString(storageConfig.ConnectionString, nil)
	if err != nil {
		panic(app.NewFatalError("Connection to AzureStorage (blob) failed", err))
	}
	return &defaultBlobInfoService{
		client.ServiceClient(),
	}
}
