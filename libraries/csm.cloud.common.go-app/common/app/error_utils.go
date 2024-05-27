package app

import (
	"github.com/rs/zerolog/log"
	"os"
)

/*
Run runs actualRoutine catching panic if it is thrown by it.
You can call it like so:

	go app.Run(func() { .. your code goes here .. })
*/
func Run(actualRoutine func()) {
	func() {
		defer HandlePanic()
		actualRoutine()
	}()
}

/*
HandlePanic merely handles the panic exiting the application with error
*/
func HandlePanic() {
	if r := recover(); r != nil {
		exitWithError(r)
	}
}

/*
exitWithError logs the error message as JSON and kills the application process.
Use panic() to throw fatal errors
*/
func exitWithError(panicAbout any) {

	// Check if it's our own error type with additional error details
	fatalErr, isFatalError := panicAbout.(FatalError)

	if isFatalError {

		// Log the error with stack trace if a stack trace is available, otherwise just log the message
		if fatalErr.error != nil {
			log.Error().Stack().Err(fatalErr.error).Msg(fatalErr.message)
		} else {
			log.Error().Msg(fatalErr.message)
		}
	} else {
		// Check if the error type is std error
		err, isError := panicAbout.(error)

		// Log the error with stack trace if a stack trace is available, otherwise just log the message
		if isError {
			log.Error().Stack().Err(err).Msg(err.Error())
		} else {
			log.Error().Msg(panicAbout.(string))
		}
	}

	os.Exit(1)
}
