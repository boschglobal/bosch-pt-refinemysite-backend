package model

type ProjectPicture struct {
	FileName                 string
	ProjectIdentifier        string
	ProjectPictureIdentifier string
	ContentType              string
}

func (p *ProjectPicture) GetAggregateType() string {
	return "PROJECTPICTURE"
}

func (p *ProjectPicture) GetBoundedContext() BoundedContext {
	return PROJECT
}

func (p *ProjectPicture) GetContentType() string {
	return p.ContentType
}

func (p *ProjectPicture) GetFileName() string {
	return p.FileName
}

func (p *ProjectPicture) GetOwnerIdentifier() string {
	return p.ProjectPictureIdentifier
}
func (p *ProjectPicture) GetOwnerType() string {
	return "PROJECT_PICTURE"
}

func (p *ProjectPicture) GetParentIdentifier() string {
	return p.ProjectIdentifier
}

func (p *ProjectPicture) GetRootContextIdentifier() string {
	return p.ProjectIdentifier
}

type TaskAttachment struct {
	FileName                 string
	ProjectIdentifier        string
	TaskIdentifier           string
	TaskAttachmentIdentifier string
	ContentType              string
}

func (t *TaskAttachment) GetAggregateType() string {
	return "TASKATTACHMENT"
}

func (t *TaskAttachment) GetBoundedContext() BoundedContext {
	return PROJECT
}

func (t *TaskAttachment) GetContentType() string {
	return t.ContentType
}

func (t *TaskAttachment) GetFileName() string {
	return t.FileName
}

func (t *TaskAttachment) GetOwnerIdentifier() string {
	return t.TaskAttachmentIdentifier
}

func (t *TaskAttachment) GetOwnerType() string {
	return "TASK_ATTACHMENT"
}

func (t *TaskAttachment) GetParentIdentifier() string {
	return t.TaskIdentifier
}

func (t *TaskAttachment) GetRootContextIdentifier() string {
	return t.ProjectIdentifier
}

type TopicAttachment struct {
	FileName                  string
	ProjectIdentifier         string
	TaskIdentifier            string
	TopicIdentifier           string
	TopicAttachmentIdentifier string
	ContentType               string
}

func (t *TopicAttachment) GetAggregateType() string {
	return "TOPICATTACHMENT"
}

func (t *TopicAttachment) GetBoundedContext() BoundedContext {
	return PROJECT
}

func (t *TopicAttachment) GetContentType() string {
	return t.ContentType
}

func (t *TopicAttachment) GetFileName() string {
	return t.FileName
}

func (t *TopicAttachment) GetOwnerIdentifier() string {
	return t.TopicAttachmentIdentifier
}

func (t *TopicAttachment) GetOwnerType() string {
	return "TOPIC_ATTACHMENT"
}

func (t *TopicAttachment) GetParentIdentifier() string {
	return t.TaskIdentifier
}

func (t *TopicAttachment) GetRootContextIdentifier() string {
	return t.ProjectIdentifier
}

type MessageAttachment struct {
	FileName                    string
	ProjectIdentifier           string
	TaskIdentifier              string
	TopicIdentifier             string
	MessageIdentifier           string
	MessageAttachmentIdentifier string
	ContentType                 string
}

func (m *MessageAttachment) GetAggregateType() string {
	return "MESSAGEATTACHMENT"
}

func (m *MessageAttachment) GetBoundedContext() BoundedContext {
	return PROJECT
}

func (m *MessageAttachment) GetContentType() string {
	return m.ContentType
}

func (m *MessageAttachment) GetFileName() string {
	return m.FileName
}

func (m *MessageAttachment) GetOwnerIdentifier() string {
	return m.MessageAttachmentIdentifier
}

func (m *MessageAttachment) GetOwnerType() string {
	return "MESSAGE_ATTACHMENT"
}

func (m *MessageAttachment) GetParentIdentifier() string {
	return m.TaskIdentifier
}

func (m *MessageAttachment) GetRootContextIdentifier() string {
	return m.ProjectIdentifier
}
