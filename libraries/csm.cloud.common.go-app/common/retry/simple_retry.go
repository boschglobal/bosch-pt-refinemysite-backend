package retry

import (
	"context"
	"fmt"
	"github.com/avast/retry-go/v4"
	"github.com/rs/zerolog/log"
	"time"
)

/*
SimpleRetryWithTracingContext retries the given function multiple times (as specified) in case of an error.
Propagates the given tracing context to the given function.
*/
func SimpleRetryWithTracingContext(method func(tracingContext context.Context) error, tracingContext context.Context, attempts uint, backoff time.Duration, name string) error {
	return SimpleRetry(func() error {
		return method(tracingContext)
	}, attempts, backoff, name)
}

/*
SimpleRetry retries the given function multiple times (as specified) in case of an error
*/
func SimpleRetry(method retry.RetryableFunc, attempts uint, backoff time.Duration, name string) error {
	return retry.Do(
		method,
		retry.Attempts(attempts),
		retry.Delay(backoff),
		retry.OnRetry(func(numberOfAttempts uint, err error) {
			log.Info().Msg(fmt.Sprintf(
				"Retry %v the %d. time. Last seen error: %q", name, numberOfAttempts+1, err))
		}))
}
