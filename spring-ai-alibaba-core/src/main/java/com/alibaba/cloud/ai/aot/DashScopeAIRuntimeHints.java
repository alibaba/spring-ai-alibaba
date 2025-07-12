package com.alibaba.cloud.ai.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import static org.springframework.ai.aot.AiRuntimeHints.findJsonAnnotatedClassesInPackage;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class DashScopeAIRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

        var mcs = MemberCategory.values();

        for (var tr : findJsonAnnotatedClassesInPackage(("com.alibaba.cloud.ai"))) {
            hints.reflection().registerType(tr, mcs);
        }
    }

}
