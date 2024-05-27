package queue

import (
	"context"
	"csm.cloud.storage.event.core/config/properties"
	storageDomain "csm.cloud.storage.event.core/domain"
	"csm.cloud.storage.event.core/kafka/producer"
	"csm.cloud.storage.event.core/storage/domain"
	"csm.cloud.storage.event.core/storage/messages/delete"
	"csm.cloud.storage.event.core/storage/messages/get"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/datadog"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/retry"
	"errors"
	"fmt"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azqueue"
	"github.com/rs/zerolog/log"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace/tracer"
	"strings"
	"time"
)

type Listener struct {
	eventProducerService producer.FileCreatedEventKafkaProducer
	getMessagesService   get.GetMessagesService
	deleteMessageService delete.DeleteMessageService
	blobInfoService      get.GetBlobInfoService
	storageConfig        properties.StorageProperties
}

func NewListener(
	eventProducerService producer.FileCreatedEventKafkaProducer,
	blobInfoService get.GetBlobInfoService,
	queueConfiguration Configuration,
) Listener {
	return Listener{
		eventProducerService: eventProducerService,
		getMessagesService:   queueConfiguration.getMessagesService,
		blobInfoService:      blobInfoService,
		deleteMessageService: queueConfiguration.deleteMessageService,
		storageConfig:        queueConfiguration.storageConfig,
	}
}

/*
Listen will continuously check for new messages and handle them in a blocking way.
*/
func (this *Listener) Listen() {

	// Run until the application closes or an error occurs
	for {
		// Load a batch of messages from azure storage queue and send kafka events
		this.retryingGetAndHandleBatchOfMessages()

		// Delay next polling interval
		time.Sleep(this.storageConfig.QueuePollingInterval)
	}
}

/*
retryingGetAndHandleBatchOfMessages wraps getAndHandleBatchOfMessages with a simple retry template
*/
func (this *Listener) retryingGetAndHandleBatchOfMessages() {

	err := retry.SimpleRetry(
		this.getAndHandleBatchOfMessages,
		this.storageConfig.QueuePollingRetryAttempts,
		this.storageConfig.QueuePollingRetryBackoff,
		"retryingGetAndHandleBatchOfMessages",
	)

	if err != nil {
		panic(app.NewFatalError("Failed to handleBatch after retries", err))
	}
}

/*
getAndHandleBatchOfMessages fetches a batch of messages using StorageQueueGet.GetMessagesService
and handles them using handleMessages method
*/
func (this *Listener) getAndHandleBatchOfMessages() error {

	// Load specified amount of messages as batch
	log.Trace().Msg("Reading next batch of file uploads.")

	options := azqueue.DequeueMessagesOptions{
		NumberOfMessages:  &this.storageConfig.QueueBatchNumberOfMessages,
		VisibilityTimeout: &this.storageConfig.QueueMessageVisibilityTimeoutInSeconds,
	}

	// Get messages from Azure Storage Queue
	requestContext, cancelFn := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancelFn()
	messages, err := this.getMessagesService.DequeueMessages(requestContext, &options)

	if err != nil {
		return err
	}

	// Handle queue messages (i.e. send kafka events)
	return this.handleMessages(messages.Messages)
}

/*
handleMessages will parse and process a batch of messages. See handleMessage for further details
*/
func (this *Listener) handleMessages(messages []*azqueue.DequeuedMessage) error {

	// Iterate over all messages and process each (send kafka event, dequeue)
	for _, message := range messages {
		err := this.handleMessage(domain.NewAzureStorageMessage(*message))
		if err != nil {
			return err
		}
	}

	return nil
}

/*
handleMessage parses and processes a single message including virus scan, event to Kafka and dequeuing from Azure
storage event queue
*/
func (this *Listener) handleMessage(message domain.AzureStorageMessage) error {

	// Convert the message into a malware scanned event
	malwareScannedEvent, err := message.ToMalwareScannedEvent()
	if err != nil {
		return err
	}

	// Parse blob information from the event's subject
	blobInfo, err := malwareScannedEvent.ToBlobInfo()
	if err != nil {
		if _, isSubjectMatchError := err.(*domain.SubjectMatchError); isSubjectMatchError {
			log.Error().Msg(fmt.Sprintf(
				"Ignoring message %q with wrong path or filename: %q", *message.MessageID, err.Error()),
			)
			// Dequeue the message from Azure storage queue as we will not process it
			return this.dequeueMessage(message)
		} else {
			return err
		}
	}

	// The one INFO log that this file is being processed
	log.Info().Msg(fmt.Sprintf("Processing file uploaded %s", blobInfo.ToString()))

	// Evaluate eventType
	if malwareScannedEvent.EventType != "Microsoft.Security.MalwareScanningResult" {
		log.Warn().Msg(fmt.Sprintf("Unexpected event type %s for blob %s!", malwareScannedEvent.EventType, blobInfo.ToString()))
		log.Info().Msg(fmt.Sprintf("Dequeuing message %s without processing", *message.MessageID))
		// Dequeue from Azure storage queue so the infected file won't be processed any further
		return this.dequeueMessage(message)
	}

	// Check malware scan result
	if malwareScannedEvent.Data.ScanResultType == "Malicious" {
		log.Warn().Msg(fmt.Sprintf("MALWARE DETECTED in blob %s!", blobInfo.ToString()))
		log.Info().Msg(fmt.Sprintf("Dequeuing message %s without processing (infected file)", *message.MessageID))
		// Dequeue from Azure storage queue so the infected file won't be processed any further
		return this.dequeueMessage(message)
	} else if malwareScannedEvent.Data.ScanResultType != "No threats found" {
		log.Error().Msg(fmt.Sprintf("Unexpected scan result %s for blob %s!", malwareScannedEvent.Data.ScanResultType, blobInfo.ToString()))
		log.Info().Msg(fmt.Sprintf("Dequeuing message %s without processing", *message.MessageID))
		// Dequeue from Azure storage queue so the infected file won't be processed any further
		return this.dequeueMessage(message)
	}

	// Send kafka messages only for uploaded images (project import files are immediately moved by
	// the project service therefore further processing will fail because of the missing blob).
	if !strings.HasPrefix(blobInfo.Path, "images") {
		log.Info().Msg(fmt.Sprintf("Skip async processing of uploaded file with path: %s/%s", blobInfo.Path, blobInfo.FileName))
		return this.dequeueMessage(message)
	}

	// Get blob properties
	blobProperties, err := this.blobInfoService.GetBlobProperties(blobInfo.ContainerName, blobInfo.Path+"/"+blobInfo.FileName)
	if err != nil {
		return err
	}

	traceHeader := blobProperties.TraceHeader
	if traceHeader == nil {
		noOpTraceHeader := "--1-"
		traceHeader = &noOpTraceHeader
	}
	parentContext, err := tracer.Extract(datadog.NewTraceHeaderStringCarrier(*traceHeader))
	if err != nil {
		log.Warn().Msg("tracing span context couldn't be determined for trace header: " + *traceHeader + " error: " + err.Error())
		parentContext = nil
	}

	// Set tracing context for traceHeader header from kafka message
	span := tracer.StartSpan("handleMessage", tracer.ChildOf(parentContext))
	span.SetBaggageItem("messageId", *message.MessageID)
	tracingContext := tracer.ContextWithSpan(context.Background(), span)

	// Check the content length of the blob content is within the allowed limit
	if blobProperties.ContentLength > this.storageConfig.MaxAllowedContentLength {
		log.Warn().Msg(fmt.Sprintf("Upload %s with content length %q exceeds the maximum allowed size of %q",
			blobInfo.ToString(),
			blobProperties.ContentLength,
			this.storageConfig.MaxAllowedContentLength,
		))
		log.Info().Msg(fmt.Sprintf("Dequeuing message %s without processing (content length)", *message.MessageID))
		span.Finish(tracer.WithError(errors.New("max file size exceeded")))
		return this.dequeueMessage(message)
	}

	// Create file created event
	fileCreatedEvent := &storageDomain.FileCreatedEvent{
		Identifier:    "/" + blobInfo.Path + "/" + blobInfo.FileName,
		Path:          "/" + blobInfo.Path,
		FileName:      blobInfo.FileName,
		ContentType:   blobProperties.ContentType,
		ContentLength: blobProperties.ContentLength,
	}

	// Send event to Kafka dequeuing the message from Azure storage queue
	_, err = datadog.TraceWithContext(tracingContext, "produce", func() (any, error) {
		return nil, this.eventProducerService.Produce(tracingContext, fileCreatedEvent)
	})

	if err != nil {
		span.Finish(tracer.WithError(err))
		return err
	}
	err = this.dequeueMessage(message)
	if err != nil {
		span.Finish(tracer.WithError(err))
		return err
	}
	span.Finish()
	return nil
}

/*
dequeueMessage removes the given message from the storage queue
*/
func (this *Listener) dequeueMessage(message domain.AzureStorageMessage) error {
	requestContext, cancelFn := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancelFn()
	_, err := this.deleteMessageService.DeleteMessage(requestContext, *message.MessageID, *message.PopReceipt, nil)
	return err
}
