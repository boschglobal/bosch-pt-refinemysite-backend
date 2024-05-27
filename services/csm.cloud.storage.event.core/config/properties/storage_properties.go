package properties

import "time"

type StorageProperties struct {
	ConnectionString                       string        `validate:"required"`
	MaxAllowedContentLength                int64         `validate:"required"`
	QueueName                              string        `validate:"required"`
	QueueBatchNumberOfMessages             int32         `validate:"required"`
	QueueMessageVisibilityTimeoutInSeconds int32         `validate:"required"`
	QueuePollingInterval                   time.Duration `validate:"required"`
	QueuePollingRetryBackoff               time.Duration `validate:"required"`
	QueuePollingRetryAttempts              uint          `validate:"required"`
}

type SharedKey struct {
	AccountName string
	AccountKey  string
}
