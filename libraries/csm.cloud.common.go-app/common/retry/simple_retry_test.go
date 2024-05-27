package retry

import (
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"testing"
	"time"
)

type Object struct {
	mock.Mock
	retryCount int
	retriesMax int
}

func newObject(maxRetries int) Object {
	return Object{
		retryCount: 0,
		retriesMax: maxRetries,
	}
}

func (o *Object) name() (any, error) {
	args := o.Called()
	if o.retryCount < o.retriesMax {
		o.retryCount += 1
		return nil, fmt.Errorf("error to trigger retry: %d", o.retryCount)
	}
	return args.Get(0), nil
}

func TestSimpleRetry(t *testing.T) {
	object := newObject(4)
	object.On("name").Return("value")

	backoff, _ := time.ParseDuration("10ms")

	assert.Nil(t, SimpleRetry(func() error {
		_, err := object.name()
		return err
	}, 5, backoff, "name"))

	object.AssertNumberOfCalls(t, "name", 5)
}

func TestSimpleRetryWithTracingContext(t *testing.T) {
	object := newObject(4)
	object.On("name").Return("value")

	ctx, cancelFn := context.WithTimeout(context.Background(), 1*time.Second)
	defer cancelFn()

	backoff, _ := time.ParseDuration("10ms")

	assert.Nil(t, SimpleRetryWithTracingContext(func(ctx context.Context) error {
		_, err := object.name()
		return err
	}, ctx, 5, backoff, "name"))

	object.AssertNumberOfCalls(t, "name", 5)
}
