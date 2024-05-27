package admin

import (
	"context"
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/rs/zerolog/log"
	"time"
)

func CreateTopics(admin *kafka.AdminClient, topics []kafka.TopicSpecification) {
	ctx, ctxCleanupFn := context.WithTimeout(context.Background(), time.Second*10)

	for _, topic := range topics {
		// Register topics individually to handle errors individually
		t := make([]kafka.TopicSpecification, 0)
		t = append(t, topic)
		topicResults, err := admin.CreateTopics(ctx, t)

		// Panic in case of a technical error
		if err != nil {
			log.Error().Msg(fmt.Sprintf("Creation of kafka topic %v failed: %v\n", topic.Topic, err))
			panic(err)
		}

		// Check the createTopics result
		for _, result := range topicResults {
			// The result can either have been: No error, TopicAlreadyExists error or any other kind of errors.
			// Ignore TopicAlreadyExists errors
			// Panic in any other error scenarios
			if result.Error.Code() != kafka.ErrNoError {
				if result.Error.Code() == kafka.ErrTopicAlreadyExists {
					log.Info().Msg(fmt.Sprintf("Creation of kafka topic %v failed, because it already exists", topic.Topic))
				} else {
					panic(result.Error)
				}

			}
		}
	}

	ctxCleanupFn()
}
