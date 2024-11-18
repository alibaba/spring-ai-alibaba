package chatmodel

import (
	"github.com/alibaba/spring-ai-alibaba/pkg/api"
	"github.com/go-resty/resty/v2"
)

type ChatModelAPI interface {
	ListChatModels(req *ListChatModelsReq) (ListChatModelsRsp, error)
	GetChatModel(req *GetChatModelReq) (*GetChatModelRsp, error)
	RunChatModel(req *RunChatModelReq) (*RunChatModelRsp, error)
}

type ChatModelAPIImpl struct {
	restClient *resty.Client
}

func NewChatModelAPI(baseURL string) ChatModelAPI {
	rc := resty.New()
	rc.SetBaseURL(baseURL + api.CommonPrefix)
	return &ChatModelAPIImpl{restClient: rc}
}

type ChatModel struct {
	Name      string `json:"name" yaml:"name"`
	Model     string `json:"model" yaml:"model"`
	ModelType string `json:"modelType" yaml:"modelType"`
}

type ChatOptions struct {
	Model             string  `json:"model" yaml:"model"`
	ProxyToolCalls    bool    `json:"proxyToolCalls" yaml:"proxyToolCalls"`
	Temperature       float32 `json:"temperature" yaml:"temperature"`
	EnableSearch      bool    `json:"enable_search" yaml:"enableSearch"`
	IncrementalOutput bool    `json:"incremental_output" yaml:"incrementalOutput"`
	MultiModel        bool    `json:"multi_model" yaml:"multiModel"`
}

type ImageOptions struct {
	Model string `json:"model"`
	N     int    `json:"n"`
}

type ListChatModelsReq struct{}

type ListChatModelsRsp []*ChatModel

func (c *ChatModelAPIImpl) ListChatModels(req *ListChatModelsReq) (ListChatModelsRsp, error) {
	const path = "/chat-models"
	r := c.restClient.R()
	rsp := &api.Resp[ListChatModelsRsp]{}
	if _, err := r.SetResult(rsp).Get(path); err != nil {
		return nil, err
	}
	if err := api.ValidateResp(rsp); err != nil {
		return nil, err
	}
	return rsp.Data, nil
}

type GetChatModelReq struct {
	ModelName string
}

type GetChatModelRsp struct {
	*ChatModel
	ChatOptions  *ChatOptions  `json:"chatOptions" yaml:"chatOptions"`
	ImageOptions *ImageOptions `json:"imageOptions" yaml:"imageOptions"`
}

func (c *ChatModelAPIImpl) GetChatModel(req *GetChatModelReq) (*GetChatModelRsp, error) {
	path := "/chat-models/" + req.ModelName
	r := c.restClient.R()
	rsp := &api.Resp[*GetChatModelRsp]{}
	if _, err := r.SetResult(rsp).Get(path); err != nil {
		return nil, err
	}
	if err := api.ValidateResp(rsp); err != nil {
		return nil, err
	}
	return rsp.Data, nil
}

type RunChatModelReq struct {
	Key          string       `json:"key"`
	Input        string       `json:"input"`
	Prompt       string       `json:"prompt"`
	UseChatModel bool         `json:"useChatModel"`
	Stream       bool         `json:"stream"`
	ChatOptions  *ChatOptions `json:"chatOptions"`
}

// RunChatModelRsp
type RunChatModelRsp struct {
	Input     *RunActionParam  `json:"input,omitempty"`
	Result    *ActionResult    `json:"result,omitempty"`
	Telemetry *TelemetryResult `json:"telemetry,omitempty"`
}

// RunActionParam
type RunActionParam struct {
	ChatOptions  *DashScopeChatOptions  `json:"chatOptions"`
	ImageOptions *DashScopeImageOptions `json:"imageOptions"`
	// user input
	Input *string `json:"input,omitempty"`
	// action key, bean name
	Key *string `json:"key,omitempty"`
	// system prompt
	Prompt *string `json:"prompt,omitempty"`
	// use stream response
	Stream *bool `json:"stream,omitempty"`
	// use chat model, is use, will be enable chat memory
	UseChatModel *bool `json:"useChatModel,omitempty"`
}

type DashScopeChatOptions struct {
	EnableSearch           *bool                    `json:"enable_search,omitempty"`
	FrequencyPenalty       *float64                 `json:"frequencyPenalty,omitempty"`
	IncrementalOutput      *bool                    `json:"incremental_output,omitempty"`
	MaxTokens              *int64                   `json:"maxTokens,omitempty"`
	Model                  *string                  `json:"model,omitempty"`
	MultiModel             *bool                    `json:"multi_model,omitempty"`
	PresencePenalty        *float64                 `json:"presencePenalty,omitempty"`
	ProxyToolCalls         *bool                    `json:"proxyToolCalls,omitempty"`
	RepetitionPenalty      *float64                 `json:"repetition_penalty,omitempty"`
	Seed                   *int64                   `json:"seed,omitempty"`
	Stop                   []map[string]interface{} `json:"stop,omitempty"`
	StopSequences          []string                 `json:"stopSequences,omitempty"`
	Temperature            *float64                 `json:"temperature,omitempty"`
	ToolChoice             map[string]interface{}   `json:"tool_choice,omitempty"`
	Tools                  []FunctionTool           `json:"tools,omitempty"`
	TopK                   *int64                   `json:"top_k,omitempty"`
	TopP                   *float64                 `json:"top_p,omitempty"`
	VlHighResolutionImages *bool                    `json:"vl_high_resolution_images,omitempty"`
}

type ToolType string

const (
	Function ToolType = "function"
)

// FunctionTool
type FunctionTool struct {
	Function *FunctionClass `json:"function,omitempty"`
	Type     *ToolType      `json:"type,omitempty"`
}

// Function
type FunctionClass struct {
	Description *string                           `json:"description,omitempty"`
	Name        *string                           `json:"name,omitempty"`
	Parameters  map[string]map[string]interface{} `json:"parameters,omitempty"`
}

type DashScopeImageOptions struct {
	Model          *string  `json:"model,omitempty"`
	N              *int64   `json:"n,omitempty"`
	NegativePrompt *string  `json:"negative_prompt,omitempty"`
	RefImg         *string  `json:"ref_img,omitempty"`
	RefMode        *string  `json:"ref_mode,omitempty"`
	RefStrength    *float64 `json:"ref_strength,omitempty"`
	ResponseFormat *string  `json:"responseFormat,omitempty"`
	Seed           *int64   `json:"seed,omitempty"`
	Size           *string  `json:"size,omitempty"`
	SizeHeight     *int64   `json:"size_height,omitempty"`
	SizeWidth      *int64   `json:"size_width,omitempty"`
	Style          *string  `json:"style,omitempty"`
}

// ActionResult
type ActionResult struct {
	Response string `json:"response"`
	// stream response
	StreamResponse []*string `json:"streamResponse"`
}

// TelemetryResult
type TelemetryResult struct {
	TraceID string `json:"traceId"`
}

// RunChatModel
func (c *ChatModelAPIImpl) RunChatModel(req *RunChatModelReq) (*RunChatModelRsp, error) {
	path := "/chat-models"
	r := c.restClient.R()
	rsp := &api.Resp[*RunChatModelRsp]{}
	if _, err := r.SetResult(rsp).SetBody(req).Post(path); err != nil {
		return nil, err
	}
	if err := api.ValidateResp(rsp); err != nil {
		return nil, err
	}
	return rsp.Data, nil
}
