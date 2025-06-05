package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.ParameterParsingNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Convert the ParameterParsing node configuration in the Dify DSL to and from the ParameterParsingNodeData object.
 */
@Component
public class ParameterParsingNodeDataConverter extends AbstractNodeDataConverter<ParameterParsingNodeData> {

    @Override
    public Boolean supportNodeType(NodeType nodeType) {
        return NodeType.PARAMETER_PARSING.equals(nodeType);
    }

    @Override
    protected List<DialectConverter<ParameterParsingNodeData>> getDialectConverters() {
        return List.of(Converter.DIFY, Converter.CUSTOM).stream()
            .map(Converter::dialectConverter)
            .toList();
    }

    private enum Converter {
        DIFY(new DialectConverter<>() {
            @SuppressWarnings("unchecked")
            @Override
            public ParameterParsingNodeData parse(Map<String, Object> data) {
                ParameterParsingNodeData nd = new ParameterParsingNodeData();

                // variable_selector -> inputs 列表
                List<String> sel = (List<String>) data.get("variable_selector");
                if (sel != null && sel.size() == 2) {
                    nd.setInputs(Collections.singletonList(
                        new VariableSelector(sel.get(0), sel.get(1))
                    ));
                }

                // input_text_key
                nd.setInputTextKey((String) data.get("input_text_key"));

                // parameters (List<Map<String, String>>)
                List<Map<String, String>> plist = (List<Map<String, String>>) data.get("parameters");
                if (plist != null) {
                    nd.setParameters(plist);
                }

                // output_key
                nd.setOutputKey((String) data.get("output_key"));

                return nd;
            }

            @Override
            public Map<String, Object> dump(ParameterParsingNodeData nd) {
                Map<String, Object> m = new LinkedHashMap<>();

                // variable_selector
                if (nd.getInputs() != null && !nd.getInputs().isEmpty()) {
                    VariableSelector vs = nd.getInputs().get(0);
                    m.put("variable_selector", List.of(vs.getNamespace(), vs.getName()));
                }

                // input_text_key
                if (nd.getInputTextKey() != null) {
                    m.put("input_text_key", nd.getInputTextKey());
                }

                // parameters
                if (nd.getParameters() != null && !nd.getParameters().isEmpty()) {
                    m.put("parameters", nd.getParameters());
                }

                // output_key
                if (nd.getOutputKey() != null) {
                    m.put("output_key", nd.getOutputKey());
                }

                return m;
            }

            @Override
            public Boolean supportDialect(DSLDialectType dialect) {
                return DSLDialectType.DIFY.equals(dialect);
            }
        }),

        CUSTOM(defaultCustomDialectConverter(ParameterParsingNodeData.class));

        private final DialectConverter<ParameterParsingNodeData> converter;

        Converter(DialectConverter<ParameterParsingNodeData> converter) {
            this.converter = converter;
        }

        public DialectConverter<ParameterParsingNodeData> dialectConverter() {
            return converter;
        }
    }
}
