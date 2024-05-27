package app

type FatalError struct {
	message string
	error   error
}

func NewFatalError(message string, error error) FatalError {
	return FatalError{
		message: message,
		error:   error,
	}
}

func (this *FatalError) Error() string {
	return "Fatal error: " + this.message
}
