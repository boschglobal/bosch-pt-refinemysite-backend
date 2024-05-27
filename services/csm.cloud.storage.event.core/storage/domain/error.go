package domain

import "fmt"

type SubjectMatchError struct {
	originalSubject string
}

func (this *SubjectMatchError) Error() string {
	return fmt.Sprintf("No match in subject of malwareScannedEvent %s", this.originalSubject)
}
