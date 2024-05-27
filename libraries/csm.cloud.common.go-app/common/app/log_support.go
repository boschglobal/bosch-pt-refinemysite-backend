package app

import (
	"fmt"
	"github.com/pkg/errors"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/rs/zerolog/pkgerrors"
	"os"
)

func init() {
	initializeLogging()
}

/*
initializeLogging - Attempts to obtain Log level from SERVICE_LOG_LEVEL environment variable defaulting to debug
*/
func initializeLogging() {

	levelInEnv := os.Getenv("SERVICE_LOG_LEVEL")
	logLevel, err := zerolog.ParseLevel(levelInEnv)

	// Determine log level
	if err != nil || logLevel == zerolog.NoLevel {
		logLevel = zerolog.DebugLevel
		log.Info().Msg(fmt.Sprintf("Defaulting to Debug logging (levelInEnv: '%s', err: %s. "+
			"Please set SERVICE_LOG_LEVEL to override", levelInEnv, err))
	}

	// Set log level and configure stack-marshaller to print stacktraces
	zerolog.SetGlobalLevel(logLevel)
	zerolog.ErrorStackMarshaler = func(err error) interface{} {
		return pkgerrors.MarshalStack(errors.WithStack(err))
	}

}
