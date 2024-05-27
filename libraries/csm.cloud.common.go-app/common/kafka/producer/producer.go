package producer

import "context"

/*
Producer abstracts a kafka producer
*/
type Producer interface {
	Produce(tracingContext context.Context, key any, value any)
}
