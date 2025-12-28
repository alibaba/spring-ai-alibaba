package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.entity.ModelConfigDO;
import com.alibaba.cloud.ai.studio.admin.service.ModelConfigBridgeService;
import com.alibaba.cloud.ai.studio.core.base.entity.ModelEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.ModelManager;
import com.alibaba.cloud.ai.studio.core.base.manager.ProviderManager;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelCredential;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ProviderConfigInfo;
import com.alibaba.cloud.ai.studio.core.utils.security.RSACryptUtils;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型配置桥接服务实现
 * 从ModelManager和ProviderManager查询数据并转换为ModelConfigDO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigBridgeServiceImpl implements ModelConfigBridgeService {

    private final ModelManager modelManager;
    private final ProviderManager providerManager;

    @Override
    public ModelConfigDO findById(Long id) {
        if (id == null) {
            return null;
        }

        try {
            // 安全地获取 workspaceId
            String workspaceId = getWorkspaceId();
            
            ModelEntity modelEntity = modelManager.findModelByIdOrName(id, workspaceId);
            if (modelEntity == null) {
                log.debug("未找到模型配置，ID: {}", id);
                return null;
            }

            return convertToModelConfigDO(modelEntity);
        } catch (Exception e) {
            log.error("查找模型配置失败，ID: {}", id, e);
            return null;
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id) != null;
    }

    @Override
    public List<ModelConfigDO> list(String name, String provider, Integer status, int offset, int limit) {
        try {
            // 查询所有模型
            List<ModelConfigInfo> modelConfigInfos;
            if (StringUtils.isNotBlank(provider)) {
                modelConfigInfos = modelManager.queryModels(provider);
            } else {
                // 如果没有指定provider，查询所有启用的模型
                modelConfigInfos = modelManager.queryEnabledModels();
            }

            // 转换为ModelConfigDO并过滤
            List<ModelConfigDO> allConfigs = modelConfigInfos.stream()
                    .map(this::convertModelConfigInfoToModelConfigDO)
                    .filter(config -> {
                        // 名称过滤
                        if (StringUtils.isNotBlank(name) && 
                            (config.getName() == null || !config.getName().contains(name))) {
                            return false;
                        }
                        // 提供商过滤
                        if (StringUtils.isNotBlank(provider) && 
                            !provider.equalsIgnoreCase(config.getProvider())) {
                            return false;
                        }
                        // 状态过滤
                        if (status != null && !status.equals(config.getStatus())) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // 分页
            int start = Math.max(offset, 0);
            int end = Math.min(start + Math.max(limit, 0), allConfigs.size());
            if (start >= allConfigs.size()) {
                return new ArrayList<>();
            }
            return allConfigs.subList(start, end);
        } catch (Exception e) {
            log.error("查询模型配置列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public int count(String name, String provider, Integer status) {
        try {
            // 查询所有模型
            List<ModelConfigInfo> modelConfigInfos;
            if (StringUtils.isNotBlank(provider)) {
                modelConfigInfos = modelManager.queryModels(provider);
            } else {
                modelConfigInfos = modelManager.queryEnabledModels();
            }

            // 转换为ModelConfigDO并过滤统计
            return (int) modelConfigInfos.stream()
                    .map(this::convertModelConfigInfoToModelConfigDO)
                    .filter(config -> {
                        // 名称过滤
                        if (StringUtils.isNotBlank(name) && 
                            (config.getName() == null || !config.getName().contains(name))) {
                            return false;
                        }
                        // 提供商过滤
                        if (StringUtils.isNotBlank(provider) && 
                            !provider.equalsIgnoreCase(config.getProvider())) {
                            return false;
                        }
                        // 状态过滤
                        if (status != null && !status.equals(config.getStatus())) {
                            return false;
                        }
                        return true;
                    })
                    .count();
        } catch (Exception e) {
            log.error("统计模型配置数量失败", e);
            return 0;
        }
    }

    @Override
    public List<ModelConfigDO> listEnabled() {
        try {
            List<ModelConfigInfo> enabledModels = modelManager.queryEnabledModels();
            return enabledModels.stream()
                    .map(this::convertModelConfigInfoToModelConfigDO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询启用的模型配置列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ModelConfigDO findByProviderAndModelId(String provider, String modelId) {
        if (StringUtils.isBlank(provider) || StringUtils.isBlank(modelId)) {
            return null;
        }

        try {
            // 直接通过provider和modelId查找ModelEntity
            // 使用queryModels获取该provider下的所有模型，然后过滤
            List<ModelConfigInfo> models = modelManager.queryModels(provider);
            for (ModelConfigInfo model : models) {
                if (modelId.equals(model.getModelId())) {
                    // 找到了匹配的ModelConfigInfo，现在需要获取对应的ModelEntity
                    String workspaceId = getWorkspaceId();
                    // 尝试通过modelId查找（可能是name或model_id）
                    ModelEntity modelEntity = modelManager.findModelByIdOrName(modelId, workspaceId);
                    // 验证provider是否匹配
                    if (modelEntity != null && provider.equals(modelEntity.getProvider())) {
                        return convertToModelConfigDO(modelEntity);
                    }
                    // 如果找不到，尝试从queryEnabledModelEntities中查找
                    List<ModelEntity> enabledEntities = modelManager.queryEnabledModelEntities();
                    for (ModelEntity entity : enabledEntities) {
                        if (provider.equals(entity.getProvider()) && modelId.equals(entity.getModelId())) {
                            return convertToModelConfigDO(entity);
                        }
                    }
                    // 如果还是找不到，使用ModelConfigInfo构建（但缺少id）
                    log.warn("找到ModelConfigInfo但未找到对应的ModelEntity，使用ModelConfigInfo构建: provider={}, modelId={}", 
                        provider, modelId);
                    return buildModelConfigDOFromModelConfigInfo(model, null);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("根据provider和modelId查找模型配置失败: provider={}, modelId={}", provider, modelId, e);
            return null;
        }
    }

    /**
     * 将ModelEntity转换为ModelConfigDO
     */
    private ModelConfigDO convertToModelConfigDO(ModelEntity modelEntity) {
        if (modelEntity == null) {
            return null;
        }

        try {
            // 获取Provider的credential（包含apiKey和endpoint）
            ProviderConfigInfo providerDetail = providerManager.getProviderDetail(modelEntity.getProvider(), false);
            if (providerDetail == null) {
                log.warn("Provider不存在: {}", modelEntity.getProvider());
                return null;
            }

            ModelCredential credential = providerDetail.getCredential();
            if (credential == null) {
                log.warn("Provider的credential不存在: {}", modelEntity.getProvider());
                return null;
            }

            // 解密apiKey
            String apiKey = credential.getApiKey();
            if (StringUtils.isNotBlank(apiKey)) {
                try {
                    apiKey = RSACryptUtils.decrypt(apiKey);
                } catch (Exception e) {
                    log.warn("解密apiKey失败，使用原始值: {}", e.getMessage());
                }
            }

            // 获取baseUrl，从credential的endpoint获取
            String baseUrl = credential.getEndpoint();
            if (StringUtils.isNotBlank(baseUrl)) {
                // 移除/v1后缀（如果存在），因为Spring AI会自动添加
                if (baseUrl.endsWith("/v1") || baseUrl.endsWith("/v1/")) {
                    baseUrl = baseUrl.replaceAll("/v1/?$", "");
                }
            } else {
                // 如果没有endpoint，使用默认值（根据provider类型）
                baseUrl = getDefaultBaseUrl(modelEntity.getProvider());
            }

            // 转换时间
            LocalDateTime createTime = convertToLocalDateTime(modelEntity.getGmtCreate());
            LocalDateTime updateTime = convertToLocalDateTime(modelEntity.getGmtModified());

            // 构建ModelConfigDO
            return ModelConfigDO.builder()
                    .id(modelEntity.getId())
                    .name(modelEntity.getName())
                    .provider(modelEntity.getProvider().toLowerCase())
                    .modelName(modelEntity.getModelId()) // ModelEntity的modelId对应ModelConfigDO的modelName
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .status(modelEntity.getEnable() != null && modelEntity.getEnable() ? 1 : 0)
                    .defaultParameters(null) // 暂时不支持，后续可以扩展
                    .supportedParameters(null) // 暂时不支持，后续可以扩展
                    .createTime(createTime)
                    .updateTime(updateTime)
                    .build();
        } catch (Exception e) {
            log.error("转换ModelEntity到ModelConfigDO失败: modelId={}", modelEntity.getModelId(), e);
            return null;
        }
    }

    /**
     * 将ModelConfigInfo转换为ModelConfigDO
     * 注意：这个方法需要查询ModelEntity来获取id等信息
     */
    private ModelConfigDO convertModelConfigInfoToModelConfigDO(ModelConfigInfo modelConfigInfo) {
        if (modelConfigInfo == null) {
            return null;
        }

        try {
            // 需要获取ModelEntity来获取完整信息
            String workspaceId = getWorkspaceId();
            String provider = modelConfigInfo.getProvider();
            String modelId = modelConfigInfo.getModelId();
            
            // 首先尝试通过modelId查找（可能是name或model_id）
            ModelEntity modelEntity = modelManager.findModelByIdOrName(modelId, workspaceId);
            
            // 验证provider是否匹配
            if (modelEntity != null && provider.equals(modelEntity.getProvider())) {
                return convertToModelConfigDO(modelEntity);
            }
            
            // 如果找不到或provider不匹配，从queryEnabledModelEntities中查找
            List<ModelEntity> enabledEntities = modelManager.queryEnabledModelEntities();
            for (ModelEntity entity : enabledEntities) {
                if (provider.equals(entity.getProvider()) && modelId.equals(entity.getModelId())) {
                    return convertToModelConfigDO(entity);
                }
            }
            
            // 如果还是找不到，尝试从queryModels中查找
            List<ModelConfigInfo> models = modelManager.queryModels(provider);
            for (ModelConfigInfo model : models) {
                if (modelId.equals(model.getModelId())) {
                    // 再次尝试查找ModelEntity
                    modelEntity = modelManager.findModelByIdOrName(modelId, workspaceId);
                    if (modelEntity != null && provider.equals(modelEntity.getProvider())) {
                        return convertToModelConfigDO(modelEntity);
                    }
                }
            }

            // 如果找不到ModelEntity，使用ModelConfigInfo构建（但缺少id）
            log.warn("找到ModelConfigInfo但未找到对应的ModelEntity: provider={}, modelId={}", 
                provider, modelId);
            return buildModelConfigDOFromModelConfigInfo(modelConfigInfo, null);
        } catch (Exception e) {
            log.error("转换ModelConfigInfo到ModelConfigDO失败: provider={}, modelId={}", 
                modelConfigInfo.getProvider(), modelConfigInfo.getModelId(), e);
            return null;
        }
    }

    /**
     * 从ModelConfigInfo构建ModelConfigDO（当找不到ModelEntity时使用）
     */
    private ModelConfigDO buildModelConfigDOFromModelConfigInfo(ModelConfigInfo modelConfigInfo, Long id) {
        try {
            ProviderConfigInfo providerDetail = providerManager.getProviderDetail(modelConfigInfo.getProvider(), false);
            if (providerDetail == null) {
                return null;
            }

            ModelCredential credential = providerDetail.getCredential();
            if (credential == null) {
                return null;
            }

            String apiKey = credential.getApiKey();
            if (StringUtils.isNotBlank(apiKey)) {
                try {
                    apiKey = RSACryptUtils.decrypt(apiKey);
                } catch (Exception e) {
                    log.warn("解密apiKey失败: {}", e.getMessage());
                }
            }

            String baseUrl = credential.getEndpoint();
            if (StringUtils.isNotBlank(baseUrl)) {
                if (baseUrl.endsWith("/v1") || baseUrl.endsWith("/v1/")) {
                    baseUrl = baseUrl.replaceAll("/v1/?$", "");
                }
            } else {
                baseUrl = getDefaultBaseUrl(modelConfigInfo.getProvider());
            }

            return ModelConfigDO.builder()
                    .id(id)
                    .name(modelConfigInfo.getName())
                    .provider(modelConfigInfo.getProvider().toLowerCase())
                    .modelName(modelConfigInfo.getModelId())
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .status(modelConfigInfo.getEnable() != null && modelConfigInfo.getEnable() ? 1 : 0)
                    .defaultParameters(null)
                    .supportedParameters(null)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("从ModelConfigInfo构建ModelConfigDO失败", e);
            return null;
        }
    }

    /**
     * 根据provider获取默认的baseUrl
     */
    private String getDefaultBaseUrl(String provider) {
        if (provider == null) {
            return "https://api.openai.com";
        }

        String lowerProvider = provider.toLowerCase();
        switch (lowerProvider) {
            case "openai":
                return "https://api.openai.com";
            case "dashscope":
            case "tongyi":
                return "https://dashscope.aliyuncs.com";
            case "deepseek":
                return "https://api.deepseek.com";
            default:
                return "https://api.openai.com";
        }
    }

    /**
     * 将Date转换为LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 安全地获取workspaceId
     */
    private String getWorkspaceId() {
        try {
            RequestContext context = RequestContextHolder.getRequestContext();
            if (context != null) {
                return context.getWorkspaceId();
            }
        } catch (Exception e) {
            log.debug("无法获取RequestContext的workspaceId: {}", e.getMessage());
        }
        return null;
    }
}

