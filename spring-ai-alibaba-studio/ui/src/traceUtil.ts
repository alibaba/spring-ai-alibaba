
interface TraceInfo {
    id: string;
    latencyMilliseconds: string;
    model: string;
    promptTokens: number;
    completionTokens: string;
    totalTokens: string;
    input: string;
    output: string;
    tags: string[];
    calculatedTotalCost: string;
    calculatedInputCost: string;
    calculatedOutputCost: string;
    usageDetails: {
        input: string;
        output: string;
        total: string;
    };
    timestamp: string;
    costDetails: {
        input: string;
        output: string;
        total: string;
    };
    traceDetail: TraceDetail;
}

interface TraceDetail {
    title: string;
    input: string;
    output: string;
    key: string;
    detail: object;
    costTime: number;
    attributes: object;
    children: TraceDetail[];
}

export function convertToTraceInfo(data: any): TraceInfo | null {
    const spans = data.scopeSpans.flatMap((scopeSpan: any) => scopeSpan.spans);

    if (spans.length < 2) return null;

    const spanMap = new Map<string, TraceDetail>();

    let totalInputTokens = 0;
    let totalOutputTokens = 0;
    let totalTokens = 0;
    let model = '';
    let input = '';
    let output = '';

    // First pass: Create TraceDetail for each span and calculate usage details
    spans.forEach((span: any) => {
        const attributes = span.attributes.reduce((acc: any, attr: any) => {
            acc[attr.key] = attr.value.stringValue;
            return acc;
        }, {});

        const prompt = extractAttributeValue(span, 'gen_ai.content.prompt') || '';
        const completion = extractAttributeValue(span, 'gen_ai.content.completion') || '';

        // Aggregate usage details
        if (attributes['gen_ai.usage.input_tokens']) {
            totalInputTokens += parseInt(attributes['gen_ai.usage.input_tokens'], 10);
        }
        if (attributes['gen_ai.usage.output_tokens']) {
            totalOutputTokens += parseInt(attributes['gen_ai.usage.output_tokens'], 10);
        }
        if (attributes['gen_ai.usage.total_tokens']) {
            totalTokens += parseInt(attributes['gen_ai.usage.total_tokens'], 10);
        }
        if (attributes['gen_ai.request.model']) {
            model = attributes['gen_ai.request.model'];
        }
        if (attributes['gen_ai.operation.name'] == 'chat') {
            model = attributes['gen_ai.request.model'];
            span.events.forEach(event => {
                const eventsAttributes = event.attributes.reduce((acc: any, attr: any) => {
                    acc[attr.key] = attr.value.arrayValue.values.map((value: any) => {
                        return { input: value.stringValue };
                    });
                    return acc;
                }, {});
                if (eventsAttributes['gen_ai.prompt']) {
                    input = eventsAttributes['gen_ai.prompt'];
                }
                if (eventsAttributes['gen_ai.completion']) {
                    output = eventsAttributes['gen_ai.completion'];
                }
            });
        }
        const costTime = span.endTimeUnixNano - span.startTimeUnixNano;
        spanMap.set(span.spanId, {
            title: `${span.name}   ${(costTime / 1e9).toFixed(2)}s`,
            input: prompt,
            key: span.spanId,
            output: completion,
            attributes: attributes,
            costTime: costTime,
            detail: {}, // Can include more info as needed
            children: [],
        });
    });

    // Second pass: Organize children
    spans.forEach((span: any) => {
        if (span.parentSpanId && spanMap.has(span.parentSpanId)) {
            const parentDetail = spanMap.get(span.parentSpanId);
            if (parentDetail) {
                parentDetail.children.push(spanMap.get(span.spanId)!);
            }
        }
    });

    const rootSpan = spans.find((span: any) => !span.parentSpanId);

    if (!rootSpan) return null;

    const rootAttributes = rootSpan.attributes.reduce((acc: any, attr: any) => {
        acc[attr.key] = attr.value.stringValue;
        return acc;
    }, {});

    return {
        id: rootSpan.traceId,
        latencyMilliseconds: calculateLatency(rootSpan.startTimeUnixNano, rootSpan.endTimeUnixNano).toString(),
        model: model,
        promptTokens: totalInputTokens,
        completionTokens: totalOutputTokens.toString(),
        totalTokens: totalTokens.toString(),
        tags: [], // Can be populated as needed
        calculatedTotalCost: '0.0', // Placeholder for actual calculation
        calculatedInputCost: '0.0', // Placeholder for actual calculation
        calculatedOutputCost: '0.0', // Placeholder for actual calculation
        usageDetails: {
            input: totalInputTokens.toString(),
            output: totalOutputTokens.toString(),
            total: totalTokens.toString(),
        },
        input: input,
        output: output,
        timestamp: rootSpan.endTimeUnixNano,
        costDetails: {
            input: '0.0', // Placeholder for actual calculation
            output: '0.0', // Placeholder for actual calculation
            total: '0.0', // Placeholder for actual calculation
        },
        traceDetail: spanMap.get(rootSpan.spanId)!,
    };
}

function extractAttributeValue(span: any, key: string): string | undefined {
    const attr = span.attributes.find((a: any) => a.key === key);
    return attr ? attr.value.stringValue : undefined;
}

function calculateLatency(startTime: string, endTime: string): number {
    return (parseInt(endTime, 10) - parseInt(startTime, 10)) / 1e6;
}


// Usage example with your input data:
// const traceDetails = convertDataToTraceDetail(yourData);

const transformTraceDetailList = (list) => {
    const res = [];

    for (const item of list) {
        const traceInfo: TraceDetail = {} as any;
        const traceDetail = {} as any;
        item.scopeSpans.spans.forEach((span) => {
            if (isRootSpan(span)) {
                traceDetail.rootSpan = span;
            }
        });
    }

    return res;
};

const isRootSpan = (span) => {
    return span.parentSpanId === undefined;
};

