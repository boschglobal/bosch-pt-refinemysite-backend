package datadog

import "github.com/rs/zerolog/log"

/*
Logger - Local implementation of ddtrace.Logger allowing to DatadogTracer to Log to correct logging framework
*/
type Logger struct {
}

/*
Log - implements ddtrace.Logger Log method
*/
func (this *Logger) Log(msg string) {
	log.Info().Msg(msg)
}
