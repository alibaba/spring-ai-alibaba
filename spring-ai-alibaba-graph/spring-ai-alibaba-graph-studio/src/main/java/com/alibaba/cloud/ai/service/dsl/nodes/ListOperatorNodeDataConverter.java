package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.ListOperatorNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convert the ListOperatorNode configuration in the Dify DSL to and from the ListOperatorNodeData object.
 */
@Component
public class ListOperatorNodeDataConverter extends AbstractNodeDataConverter<ListOperatorNodeData> {

    @Override
    public Boolean supportNodeType(NodeType nodeType) {
        return NodeType.LIST_OPERATOR.equals(nodeType);
    }

    @Override
    protected List<DialectConverter<ListOperatorNodeData>> getDialectConverters() {
        return Stream.of(Converter.DIFY, Converter.CUSTOM)
                     .map(Converter::dialectConverter)
                     .collect(Collectors.toList());
    }

    private enum Converter {
        DIFY(new DialectConverter<>() {
            @SuppressWarnings("unchecked")
            @Override
            public ListOperatorNodeData parse(Map<String, Object> data) {
                ListOperatorNodeData nd = new ListOperatorNodeData();

                // inputs => variable_selector
                List<String> sel = (List<String>) data.get("variable_selector");
                if (sel != null && sel.size() == 2) {
                    nd.setInputs(Collections.singletonList(
                        new VariableSelector(sel.get(0), sel.get(1))
                    ));
                }

                // input_text_key
                nd.setInputTextKey((String) data.get("input_text_key"));

                // output_text_key
                nd.setOutputTextKey((String) data.get("output_text_key"));

                // filters（List<String>）
                List<String> fl = (List<String>) data.get("filters");
                nd.setFilters(fl != null ? fl : Collections.emptyList());

                // comparators（List<String>）
                List<String> cm = (List<String>) data.get("comparators");
                nd.setComparators(cm != null ? cm : Collections.emptyList());

                // limit_number
                if (data.get("limit_number") != null) {
                    nd.setLimitNumber(((Number) data.get("limit_number")).longValue());
                }

                // element_class_type
                nd.setElementClassType((String) data.get("element_class_type"));

                return nd;
            }

            @Override
            public Map<String, Object> dump(ListOperatorNodeData nd) {
                Map<String, Object> m = new LinkedHashMap<>();

                // variable_selector
                if (!nd.getInputs().isEmpty()) {
                    VariableSelector vs = nd.getInputs().get(0);
                    m.put("variable_selector", List.of(vs.getNamespace(), vs.getName()));
                }
                // input_text_key
                if (nd.getInputTextKey() != null) {
                    m.put("input_text_key", nd.getInputTextKey());
                }
                // output_text_key
                if (nd.getOutputTextKey() != null) {
                    m.put("output_text_key", nd.getOutputTextKey());
                }
                // filters
                if (nd.getFilters() != null && !nd.getFilters().isEmpty()) {
                    m.put("filters", nd.getFilters());
                }
                // comparators
                if (nd.getComparators() != null && !nd.getComparators().isEmpty()) {
                    m.put("comparators", nd.getComparators());
                }
                // limit_number
                if (nd.getLimitNumber() != null) {
                    m.put("limit_number", nd.getLimitNumber());
                }
                // element_class_type
                if (nd.getElementClassType() != null) {
                    m.put("element_class_type", nd.getElementClassType());
                }
                return m;
            }

            @Override
            public Boolean supportDialect(DSLDialectType dialect) {
                return DSLDialectType.DIFY.equals(dialect);
            }
        }),
        CUSTOM(defaultCustomDialectConverter(ListOperatorNodeData.class));

        private final DialectConverter<ListOperatorNodeData> converter;

        Converter(DialectConverter<ListOperatorNodeData> converter) {
            this.converter = converter;
        }

        public DialectConverter<ListOperatorNodeData> dialectConverter() {
            return converter;
        }
    }
}
