import {CircleNode, CircleNodeModel, h, PolygonNode, PolygonNodeModel, RectNode, RectNodeModel,} from '@logicflow/core';

// ============ Node Definitions (match backend FlowNode TYPE_) ============
// TYPE: start | end | llm | condition | http | template | parallel | agent

// 开始节点 (CircleNode)
class StartNode extends CircleNode {
    static extendKey = 'start';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#52c41a';
        style.stroke = '#52c41a';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#fff';
        style.fontSize = 14;
        return style;
    }
}

// LLM 节点 (矩形)
class LLMNode extends RectNode {
    static extendKey = 'llm';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#e6f4ff';
        style.stroke = '#1677ff';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#1677ff';
        style.fontSize = 13;
        return style;
    }
}

// Agent 节点 (矩形)
class AgentNode extends RectNode {
    static extendKey = 'agent';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#f0f5ff';
        style.stroke = '#2f54eb';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#2f54eb';
        style.fontSize = 13;
        return style;
    }
}

// 条件分支节点 (菱形)
class ConditionNode extends PolygonNode {
    static extendKey = 'condition';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#fff7e6';
        style.stroke = '#fa8c16';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#d46b08';
        style.fontSize = 12;
        return style;
    }

    getAttributes() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const attributes = super.getAttributes();
        const width = 120;
        const height = 80;
        const points = [
            [attributes.x, attributes.y - height / 2],
            [attributes.x + width / 2, attributes.y],
            [attributes.x, attributes.y + height / 2],
            [attributes.x - width / 2, attributes.y],
        ];
        return {...attributes, points, width, height};
    }
}

// HTTP 节点 (矩形)
class HttpNode extends RectNode {
    static extendKey = 'http';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#fff7e6';
        style.stroke = '#fa8c16';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#d46b08';
        style.fontSize = 13;
        return style;
    }
}

// 模板/变量节点 (矩形)
class TemplateNode extends RectNode {
    static extendKey = 'template';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#f6ffed';
        style.stroke = '#13c2c2';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#08979c';
        style.fontSize = 13;
        return style;
    }
}

// 并行节点 (六边形-ish rect)
class ParallelNode extends RectNode {
    static extendKey = 'parallel';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#f9f0ff';
        style.stroke = '#722ed1';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#722ed1';
        style.fontSize = 13;
        return style;
    }

    getShape() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const {x, y, width, height} = this.getAttributes();
        const w = width / 2;
        const halfH = height / 2;        // use halfH to avoid shadowing the imported `h` SVG helper
        const offset = 12;
        return h('polygon', {
            points: [
                [x - w + offset, y - halfH],
                [x + w - offset, y - halfH],
                [x + w, y],
                [x + w - offset, y + halfH],
                [x - w + offset, y + halfH],
                [x - w, y],
            ],
        });
    }
}

// 结束节点 (CircleNode)
class EndNode extends CircleNode {
    static extendKey = 'end';

    getNodeStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getNodeStyle();
        style.fill = '#ff4d4f';
        style.stroke = '#ff4d4f';
        style.strokeWidth = 2;
        return style;
    }

    getTextStyle() {
        // @ts-ignore – LogicFlow runtime method not in type defs
        const style = super.getTextStyle();
        style.color = '#fff';
        style.fontSize = 14;
        return style;
    }
}

// ============ Register All Nodes ============
// LogicFlow 2.x：View 与 Model 是两类，必须分别传入。
// 之前用 `CircleNode.model` 是错误的 —— 那是 View 类，并没有静态 model 属性，
// 导致 lf.render() 时报「找不到start对应的节点」。
export function registerAgentNodes(lf: any): void {
    lf.register({type: 'start', view: StartNode, model: CircleNodeModel});
    lf.register({type: 'llm', view: LLMNode, model: RectNodeModel});
    lf.register({type: 'agent', view: AgentNode, model: RectNodeModel});
    lf.register({type: 'condition', view: ConditionNode, model: PolygonNodeModel});
    lf.register({type: 'http', view: HttpNode, model: RectNodeModel});
    lf.register({type: 'template', view: TemplateNode, model: RectNodeModel});
    lf.register({type: 'parallel', view: ParallelNode, model: RectNodeModel});
    lf.register({type: 'end', view: EndNode, model: CircleNodeModel});
}

// ============ Node Palette (Draggable Types) ============
export const AGENT_FLOW_NODE_TYPES = [
    {type: 'start', label: '开始', icon: 'https://cdn.jsdelivr.net/gh/LogicFlow/static@latest/docs/static/start.png'},
    {type: 'llm', label: 'LLM 调用', icon: '🤖'},
    {type: 'agent', label: 'Agent 节点', icon: '🤖'},
    {
        type: 'condition',
        label: '条件分支',
        icon: 'https://cdn.jsdelivr.net/gh/LogicFlow/static@latest/docs/static/condition.png'
    },
    {type: 'http', label: 'HTTP 请求', icon: '🔗'},
    {type: 'template', label: '模板转换', icon: '📝'},
    {type: 'parallel', label: '并行执行', icon: '⚡'},
    {type: 'end', label: '结束', icon: 'https://cdn.jsdelivr.net/gh/LogicFlow/static@latest/docs/static/end.png'},
];
