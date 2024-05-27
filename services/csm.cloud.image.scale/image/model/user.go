package model

type ProfilePicture struct {
	FileName                 string
	UserIdentifier           string
	ProfilePictureIdentifier string
	ContentType              string
}

func (p *ProfilePicture) GetAggregateType() string {
	return "USERPICTURE"
}

func (p *ProfilePicture) GetBoundedContext() BoundedContext {
	return USER
}

func (p *ProfilePicture) GetContentType() string {
	return p.ContentType
}

func (p *ProfilePicture) GetFileName() string {
	return p.FileName
}

func (p *ProfilePicture) GetOwnerIdentifier() string {
	return p.ProfilePictureIdentifier
}
func (p *ProfilePicture) GetOwnerType() string {
	return "USER_PICTURE"
}

func (p *ProfilePicture) GetParentIdentifier() string {
	return p.UserIdentifier
}

func (p *ProfilePicture) GetRootContextIdentifier() string {
	return p.UserIdentifier
}
