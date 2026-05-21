package io.github.kentasun.stepflow.flow.dto.node;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.Nulls;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;
import lombok.Getter;

/**
 * 流程类的公共父类，所有流程实现类都必须继承该类
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY, // 告诉 Jackson 这个字段类里本来就有
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StepFlowNode.class, name = FlowContentType.STEP),
        @JsonSubTypes.Type(value = SubFlowNode.class, name = FlowContentType.SUB_FLOW),
        @JsonSubTypes.Type(value = SequenceFlowNode.class, name = FlowContentType.SEQUENCE),
        @JsonSubTypes.Type(value = ParallelFlowNode.class, name = FlowContentType.PARALLEL),
        @JsonSubTypes.Type(value = IfElseFlowNode.class, name = FlowContentType.IF_ELSE)
        // 添加2种循环的 flow
})
public abstract class FlowNode {

    // 节点类型
    @JsonSetter(nulls = Nulls.FAIL)
    @Getter
    protected final String type;

    // 强迫子类在创建时必须明确给出自己的类型
    protected FlowNode(String type) {
        this.type = type;
    }

    public abstract void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext);

    /**
     * 递归校验节点合法性
     *
     * @param context 递归校验上下文
     * @param globalFlowCode 此节点所属的 {@code Flow} 的步骤标识
     */
    public abstract void validate(FlowNodeValidateContext context, String globalFlowCode);
}
