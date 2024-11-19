package chatmodel

type ChatModelAPI interface {
	ListChatModels(req *ListChatModelsReq) (ListChatModelsRsp, error)
	GetChatModel(req *GetChatModelReq) (*GetChatModelRsp, error)
	RunChatModel(req *RunChatModelReq) (*RunChatModelRsp, error)
}
