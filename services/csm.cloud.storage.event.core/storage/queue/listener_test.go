package queue

import (
	"context"
	"csm.cloud.storage.event.core/config"
	"csm.cloud.storage.event.core/domain"
	"csm.cloud.storage.event.core/storage/messages/delete"
	"csm.cloud.storage.event.core/storage/messages/get"
	commonConfig "dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config"
	"encoding/base64"
	"errors"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azqueue"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"os"
	"testing"
	"time"
)

// Define FileCreatedEvent mock

type FileCreatedEventProducerMock struct {
	mock.Mock
}

func (this *FileCreatedEventProducerMock) Produce(tracingContext context.Context, event *domain.FileCreatedEvent) error {
	args := this.Called(tracingContext, event)
	return args.Error(0)
}

// Define DeleteMessageService mock

type DeleteMessageServiceMock struct {
	mock.Mock
}

func (this *DeleteMessageServiceMock) DeleteMessage(ctx context.Context, messageID string, popReceipt string, o *azqueue.DeleteMessageOptions) (azqueue.DeleteMessageResponse, error) {
	args := this.Called(ctx, messageID, popReceipt, o)
	return azqueue.DeleteMessageResponse{}, args.Error(1)
}

// Define GetMessageService mock

type GetMessageServiceMock struct {
	mock.Mock
}

func (this *GetMessageServiceMock) DequeueMessages(ctx context.Context, options *azqueue.DequeueMessagesOptions) (azqueue.DequeueMessagesResponse, error) {
	args := this.Called(ctx, options)
	return args.Get(0).(azqueue.DequeueMessagesResponse), args.Error(1)
}

type GetBlobInfoServiceMock struct {
	mock.Mock
}

func (this *GetBlobInfoServiceMock) GetBlobProperties(container string, blobName string) (*get.BlobProperties, error) {
	args := this.Called(container, blobName)
	response := args.Get(0).(get.BlobProperties)
	return &response, args.Error(1)
}

// Tests

func TestHandleMessages_One_Message_Produced(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}
	producerMock.On("Produce", mock.Anything, mock.Anything).Return(nil)

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}
	blobInfoServiceMock.On("GetBlobProperties", mock.Anything, mock.Anything).Return(get.BlobProperties{ContentLength: 524288, ContentType: "text/plain"}, nil)

	// Init test configurations
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessage()
	messages = append(messages, &message)

	// Process messages and verify that the message was produced successfully
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	assert.Nil(t, err)
	assert.Equal(t, 1, len(producerMock.Calls),
		"One message is expected to be produced to kafka")
	assert.Equal(t, 1, len(deleteMessageServiceMock.Calls),
		"One message is expected to be deleted from the message queue")
	assert.Equal(t, 1, len(blobInfoServiceMock.Calls),
		"One blob properties call is expected to be performed")
}

func TestHandleMessages_NoSubjectMatched(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}

	// Init test configurations
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessageWithNoSubjectMatch()
	messages = append(messages, &message)

	// Process messages and verify that message was ignored if subject doesn't match
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	assert.Nil(t, err)

	deleteMessageServiceMock.AssertExpectations(t)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 0)

	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 0)
}

func TestHandleMessages_SkipInvalidDirectory(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}

	// Init test configurations
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessageSkipDirectory()
	messages = append(messages, &message)

	// Process messages and verify that message was ignored if subject doesn't match
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	assert.Nil(t, err)

	deleteMessageServiceMock.AssertExpectations(t)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 0)

	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 0)
}

func TestHandleMessages_InvalidText(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}
	deleteMessageServiceMock := &DeleteMessageServiceMock{}

	blobInfoServiceMock := &GetBlobInfoServiceMock{}

	// Init test configurations
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessageWithBrokenBase64Text()
	messages = append(messages, &message)

	// Process messages and verify that processing failed if message contains invalid encoded content
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	// Verify an error is returned
	assert.IsType(t, base64.CorruptInputError(0), err)
	assert.Equal(t, "illegal base64 data at input byte 3", err.Error())

	// Verify the message is not processed any further
	deleteMessageServiceMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "DeleteMessage", 0)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 0)

	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 0)
}

func TestHandleMessages_MalwareFound(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}

	// Init test configuration
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessageWithMalwareFound()
	messages = append(messages, &message)

	// Process messages
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	// Verify messages with malware are handled without error
	assert.Nil(t, err)

	// Verify that no event is emitted for files containing malware
	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 0)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 0)

	deleteMessageServiceMock.AssertExpectations(t)
	deleteMessageServiceMock.AssertNumberOfCalls(t, "DeleteMessage", 1)
}

func TestHandleMessages_UnexpectedScanResultType(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}

	// Init test configuration
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessageWithUnexpectedScanResultType()
	messages = append(messages, &message)

	// Process messages
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	// Verify messages with malware are handled without error
	assert.Nil(t, err)

	// Verify that no event is emitted for files containing malware
	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 0)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 0)

	deleteMessageServiceMock.AssertExpectations(t)
	deleteMessageServiceMock.AssertNumberOfCalls(t, "DeleteMessage", 1)
}

func TestHandleMessages_UnexpectedEventType(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}

	// Init test configuration
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessageWithUnexpectedEventType()
	messages = append(messages, &message)

	// Process messages
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	// Verify messages with malware are handled without error
	assert.Nil(t, err)

	// Verify that no event is emitted for files containing malware
	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 0)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 0)

	deleteMessageServiceMock.AssertExpectations(t)
	deleteMessageServiceMock.AssertNumberOfCalls(t, "DeleteMessage", 1)
}

func TestHandleMessages_MaxContentLengthExceeded(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}
	blobInfoServiceMock.On("GetBlobProperties", mock.Anything, mock.Anything).Return(get.BlobProperties{ContentLength: 629145600, ContentType: "text/plain"}, nil)

	// Init test configuration
	testConfiguration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(testConfiguration, deleteMessageServiceMock, nil)

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessage()
	messages = append(messages, &message)

	// Process messages
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	err := queueListener.handleMessages(messages)

	// Verify messages with content length exceeding the allowed limit are handled without error
	assert.Nil(t, err)

	// Verify that no event is emitted for files where content length exceeded
	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 0)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 1)

	deleteMessageServiceMock.AssertExpectations(t)
	deleteMessageServiceMock.AssertNumberOfCalls(t, "DeleteMessage", 1)
}

func TestListener_retryingHandleBatchOfMessages_WithRetryOnFailingProducer(t *testing.T) {

	// Init test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessage()
	messages = append(messages, &message)

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}
	producerMock.On("Produce", mock.Anything, mock.Anything).Return(errors.New("SOME_KAFKA_AVAILABILITY_ISSUE")).Once()
	producerMock.On("Produce", mock.Anything, mock.Anything).Return(nil)

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	getMessageServiceMock := &GetMessageServiceMock{}
	getMessageServiceMock.On("DequeueMessages", mock.Anything, mock.Anything).Return(azqueue.DequeueMessagesResponse{Messages: messages}, nil).Times(2)
	getMessageServiceMock.On("DequeueMessages", mock.Anything, mock.Anything).Return(azqueue.DequeueMessagesResponse{}, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}
	blobInfoServiceMock.On("GetBlobProperties", mock.Anything, mock.Anything).Return(get.BlobProperties{ContentLength: 524288, ContentType: "text/plain"}, nil)

	// Init test configuration
	configuration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(configuration, deleteMessageServiceMock, getMessageServiceMock)

	// Process messages
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	queueListener.retryingGetAndHandleBatchOfMessages()

	// Verify that messages are fetched and producing is attempted twice
	deleteMessageServiceMock.AssertExpectations(t)
	deleteMessageServiceMock.AssertNumberOfCalls(t, "DeleteMessage", 1)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 2)

	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 2)

	getMessageServiceMock.AssertExpectations(t)
	getMessageServiceMock.AssertNumberOfCalls(t, "DequeueMessages", 2)
}

func TestListener_retryingHandleBatchOfMessages_WithRetryOnFailingGetMessages(t *testing.T) {

	// Init test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// Init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessage()
	messages = append(messages, &message)

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}
	producerMock.On("Produce", mock.Anything, mock.Anything).Return(nil)

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	getMessageServiceMock := &GetMessageServiceMock{}
	getMessageServiceMock.On("DequeueMessages", mock.Anything, mock.Anything).Return(azqueue.DequeueMessagesResponse{}, errors.New("NETWORK_ERROR")).Times(3)
	getMessageServiceMock.On("DequeueMessages", mock.Anything, mock.Anything).Return(azqueue.DequeueMessagesResponse{Messages: messages}, nil).Once()
	getMessageServiceMock.On("DequeueMessages", mock.Anything, mock.Anything).Return(azqueue.DequeueMessagesResponse{}, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}
	blobInfoServiceMock.On("GetBlobProperties", mock.Anything, mock.Anything).Return(get.BlobProperties{ContentLength: 524288, ContentType: "text/plain"}, nil)

	// Init test configuration
	configuration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(configuration, deleteMessageServiceMock, getMessageServiceMock)

	// Process messages
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	queueListener.retryingGetAndHandleBatchOfMessages()

	// Verify that only one event is scanned, produced and one message dequeued while multiple attempts to get
	// more messages after initial failure are observed
	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", 1)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", 1)

	deleteMessageServiceMock.AssertExpectations(t)
	deleteMessageServiceMock.AssertNumberOfCalls(t, "DeleteMessage", 1)

	getMessageServiceMock.AssertExpectations(t)
	getMessageServiceMock.AssertNumberOfCalls(t, "DequeueMessages", 4)
}

func TestListener_Listen(t *testing.T) {

	// Activate test profile
	_ = os.Setenv("GO_PROFILES_ACTIVE", "test")

	// init messages
	var messages []*azqueue.DequeuedMessage
	message := createTestMessage()
	messages = append(messages, &message)

	// Init mocks
	producerMock := &FileCreatedEventProducerMock{}
	producerMock.On("Produce", mock.Anything, mock.Anything).Return(nil)

	deleteMessageServiceMock := &DeleteMessageServiceMock{}
	deleteMessageServiceMock.On("DeleteMessage", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	getMessageServiceMock := &GetMessageServiceMock{}
	getMessageServiceMock.On("DequeueMessages", mock.Anything, mock.Anything).Return(azqueue.DequeueMessagesResponse{Messages: messages}, nil)

	blobInfoServiceMock := &GetBlobInfoServiceMock{}
	blobInfoServiceMock.On("GetBlobProperties", mock.Anything, mock.Anything).Return(get.BlobProperties{ContentLength: 524288, ContentType: "text/plain"}, nil)

	// Init test configurations
	configuration := LoadTestConfigurationFromFilesystem()
	testQueueConfiguration := NewTestQueueConfiguration(configuration, deleteMessageServiceMock, getMessageServiceMock)

	// Run listener asynchronously and process messages
	queueListener := NewListener(producerMock, blobInfoServiceMock, testQueueConfiguration)
	go func() {
		queueListener.Listen()
	}()

	// Wait half a second to ensure that the message was processed successfully
	time.Sleep(500 * time.Millisecond)

	// Verify that multiple messages were processed successfully
	getMessageServiceMock.AssertExpectations(t)
	getMessageServiceMock.AssertNumberOfCalls(t, "DequeueMessages", 10)
	numberOfInvocations := len(getMessageServiceMock.Calls)
	assert.True(t, len(getMessageServiceMock.Calls) > 8)

	blobInfoServiceMock.AssertExpectations(t)
	blobInfoServiceMock.AssertNumberOfCalls(t, "GetBlobProperties", numberOfInvocations)

	deleteMessageServiceMock.AssertExpectations(t)
	deleteMessageServiceMock.AssertNumberOfCalls(t, "DeleteMessage", numberOfInvocations)

	producerMock.AssertExpectations(t)
	producerMock.AssertNumberOfCalls(t, "Produce", numberOfInvocations)
}

func createTestMessage() azqueue.DequeuedMessage {
	text := `{
		"id": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
		"subject": "storageAccounts/defendermalwaretest/containers/csm-quarantine-container/blobs/images/projects/60098f64-f566-49c6-86d8-1071eaebc6a3/picture/76b390f8-f66c-43d1-b181-c703ec817110",
		"data": {
			"correlationId": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
			"blobUri": "https://defendermalwaretest.blob.core.windows.net/csm-quarantine-container/images/projects/60098f64-f566-49c6-86d8-1071eaebc6a3/picture/76b390f8-f66c-43d1-b181-c703ec817110",
			"eTag": "0x8DBCFD701F78E3B",
			"scanFinishedTimeUtc": "2023-10-18T12:37:42.8034649Z",
			"scanResultType": "No threats found",
			"scanResultDetails": null
		},
		"eventType": "Microsoft.Security.MalwareScanningResult",
		"dataVersion": "1.0",
		"metadataVersion": "1",
		"eventTime": "2023-10-18T12:37:42.8040405Z",
		"topic": "/subscriptions/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/resourceGroups/defender-malware-test/providers/Microsoft.EventGrid/topics/defendermalwaretest-eventgrid-topic"
	  }`
	return newMessage(base64.URLEncoding.EncodeToString([]byte(text)))
}

func createTestMessageSkipDirectory() azqueue.DequeuedMessage {
	text := `{
		"id": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
		"subject": "storageAccounts/defendermalwaretest/containers/csm-quarantine-container/blobs/not-images/projects/60098f64-f566-49c6-86d8-1071eaebc6a3/picture/76b390f8-f66c-43d1-b181-c703ec817110",
		"data": {
			"correlationId": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
			"blobUri": "https://defendermalwaretest.blob.core.windows.net/csm-quarantine-container/not-images/projects/60098f64-f566-49c6-86d8-1071eaebc6a3/picture/76b390f8-f66c-43d1-b181-c703ec817110",
			"eTag": "0x8DBCFD701F78E3B",
			"scanFinishedTimeUtc": "2023-10-18T12:37:42.8034649Z",
			"scanResultType": "No threats found",
			"scanResultDetails": null
		},
		"eventType": "Microsoft.Security.MalwareScanningResult",
		"dataVersion": "1.0",
		"metadataVersion": "1",
		"eventTime": "2023-10-18T12:37:42.8040405Z",
		"topic": "/subscriptions/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/resourceGroups/defender-malware-test/providers/Microsoft.EventGrid/topics/defendermalwaretest-eventgrid-topic"
	  }`
	return newMessage(base64.URLEncoding.EncodeToString([]byte(text)))
}

func createTestMessageWithNoSubjectMatch() azqueue.DequeuedMessage {
	text := `{
		"id": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
		"subject": "storageAccounts/defendermalwaretest/containers/uploads/blobs/new-file.txt",
		"data": {
			"correlationId": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
			"blobUri": "https://defendermalwaretest.blob.core.windows.net/uploads/new-file.txt",
			"eTag": "0x8DBCFD701F78E3B",
			"scanFinishedTimeUtc": "2023-10-18T12:37:42.8034649Z",
			"scanResultType": "No threats found",
			"scanResultDetails": null
		},
		"eventType": "Microsoft.Security.MalwareScanningResult",
		"dataVersion": "1.0",
		"metadataVersion": "1",
		"eventTime": "2023-10-18T12:37:42.8040405Z",
		"topic": "/subscriptions/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/resourceGroups/defender-malware-test/providers/Microsoft.EventGrid/topics/defendermalwaretest-eventgrid-topic"
	  }`
	return newMessage(base64.URLEncoding.EncodeToString([]byte(text)))
}

func createTestMessageWithBrokenBase64Text() azqueue.DequeuedMessage {
	text := "NOT_A_BASE_64"
	return newMessage(text)
}

func createTestMessageWithMalwareFound() azqueue.DequeuedMessage {
	text := `{
		"id": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
		"subject": "storageAccounts/defendermalwaretest/containers/uploads/blobs/my-folder/new-file.txt",
		"data": {
			"correlationId": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
			"blobUri": "https://defendermalwaretest.blob.core.windows.net/uploads/my-folder/new-file.txt",
			"eTag": "0x8DBCFD701F78E3B",
			"scanFinishedTimeUtc": "2023-10-18T12:37:42.8034649Z",
			"scanResultType": "Malicious",
			"scanResultDetails": {
				"malwareNamesFound": [
					"DOS/EICAR_Test_File"
				],
				"sha256": "275A021BBFB6489E54D471899F7DB9D1663FC695EC2FE2A2C4538AABF651FD0F"
			}
		},
		"eventType": "Microsoft.Security.MalwareScanningResult",
		"dataVersion": "1.0",
		"metadataVersion": "1",
		"eventTime": "2023-10-18T12:37:42.8040405Z",
		"topic": "/subscriptions/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/resourceGroups/defender-malware-test/providers/Microsoft.EventGrid/topics/defendermalwaretest-eventgrid-topic"
	  }`
	return newMessage(base64.URLEncoding.EncodeToString([]byte(text)))
}

func createTestMessageWithUnexpectedScanResultType() azqueue.DequeuedMessage {
	text := `{
		"id": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
		"subject": "storageAccounts/defendermalwaretest/containers/uploads/blobs/my-folder/new-file.txt",
		"data": {
			"correlationId": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
			"blobUri": "https://defendermalwaretest.blob.core.windows.net/uploads/my-folder/new-file.txt",
			"eTag": "0x8DBCFD701F78E3B",
			"scanFinishedTimeUtc": "2023-10-18T12:37:42.8034649Z",
			"scanResultType": "Unexpected",
			"scanResultDetails": {
				"malwareNamesFound": [
					"DOS/EICAR_Test_File"
				],
				"sha256": "275A021BBFB6489E54D471899F7DB9D1663FC695EC2FE2A2C4538AABF651FD0F"
			}
		},
		"eventType": "Microsoft.Security.MalwareScanningResult",
		"dataVersion": "1.0",
		"metadataVersion": "1",
		"eventTime": "2023-10-18T12:37:42.8040405Z",
		"topic": "/subscriptions/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/resourceGroups/defender-malware-test/providers/Microsoft.EventGrid/topics/defendermalwaretest-eventgrid-topic"
	  }`
	return newMessage(base64.URLEncoding.EncodeToString([]byte(text)))
}

func createTestMessageWithUnexpectedEventType() azqueue.DequeuedMessage {
	text := `{
		"id": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
		"subject": "storageAccounts/defendermalwaretest/containers/uploads/blobs/my-folder/new-file.txt",
		"data": {
			"correlationId": "2209bebf-9e38-4fdd-bf9c-5842129d8f63",
			"blobUri": "https://defendermalwaretest.blob.core.windows.net/uploads/my-folder/new-file.txt",
			"eTag": "0x8DBCFD701F78E3B"
		},
		"eventType": "Microsoft.Security.Unexpected",
		"dataVersion": "1.0",
		"metadataVersion": "1",
		"eventTime": "2023-10-18T12:37:42.8040405Z",
		"topic": "/subscriptions/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/resourceGroups/defender-malware-test/providers/Microsoft.EventGrid/topics/defendermalwaretest-eventgrid-topic"
	  }`
	return newMessage(base64.URLEncoding.EncodeToString([]byte(text)))
}

func newMessage(text string) azqueue.DequeuedMessage {
	id := "1"
	var dequeueCount int64 = 0
	expirationTime := time.Now()
	insertionTime := time.Now()
	nextVisibleTime := time.Now()
	popReceipt := ""
	return azqueue.DequeuedMessage{
		DequeueCount:    &dequeueCount,
		ExpirationTime:  &expirationTime,
		InsertionTime:   &insertionTime,
		MessageID:       &id,
		MessageText:     &text,
		PopReceipt:      &popReceipt,
		TimeNextVisible: &nextVisibleTime,
	}
}

func NewTestQueueConfiguration(
	configuration config.Configuration,
	deleteMessageService delete.DeleteMessageService,
	getMessagesService get.GetMessagesService,
) Configuration {
	return Configuration{
		storageConfig:        configuration.Storage,
		deleteMessageService: deleteMessageService,
		getMessagesService:   getMessagesService,
	}
}

func LoadTestConfigurationFromFilesystem() config.Configuration {
	conf, err := commonConfig.LoadConfiguration(config.Configuration{}, "../../")
	if err != nil {
		panic(err)
	}

	return conf.(config.Configuration)
}
