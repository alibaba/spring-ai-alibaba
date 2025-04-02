package com.alibaba.cloud.ai.example.manus.dynamic.agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;

@Repository
public interface DynamicAgentRepository extends JpaRepository<DynamicAgentEntity, Long> {

	DynamicAgentEntity findByAgentName(String agentName);

}
