package kafka

import (
	"context"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"time"
)

type Record struct {
	Ctx     context.Context
	Message DeserializedMessage
}

type DeserializedMessage struct {
	Headers        []kafka.Header
	TopicPartition kafka.TopicPartition
	Timestamp      time.Time
	TimestampType  kafka.TimestampType
	Key            any
	Value          any
}
