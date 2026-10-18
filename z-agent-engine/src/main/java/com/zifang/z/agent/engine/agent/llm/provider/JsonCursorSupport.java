package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.util.json.JsonCursor;

/**
 * {@link JsonCursor} 的取值小工具。
 * <p>
 * 迁移自 Jackson {@code JsonNode} 时，{@code node.path("x").asText(null)} 这种
 * “取不到就当 null” 的写法需要一个显式形态 —— {@link JsonCursor#asText()} 对
 * missing 只返回空串，不保留 Jackson 的 null 语义。这些 provider 的协议字段
 * （tool_call id/name、arguments）都是“缺失就留空”的，故集中在此还原原语义。
 */
final class JsonCursorSupport {

    private JsonCursorSupport() {
    }

    /** 缺失或 JSON null 返回 {@code null}，否则返回标量文本（与 {@code JsonNode.asText(null)} 一致）。 */
    static String textOrNull(JsonCursor cursor) {
        if (cursor == null || cursor.isMissing() || cursor.isNull()) {
            return null;
        }
        return cursor.asText();
    }

    /** 缺失或 JSON null 返回 {@code null}，否则返回 {@code Integer}（与 {@code JsonNode.asInt()} 装箱语义一致）。 */
    static Integer integerOrNull(JsonCursor cursor) {
        if (cursor == null || cursor.isMissing() || cursor.isNull()) {
            return null;
        }
        return cursor.asInt(0);
    }

    /** 游标是否指向一个 JSON 字符串（Jackson 的 {@code JsonNode.isTextual()}）。 */
    static boolean isTextual(JsonCursor cursor) {
        return cursor != null && !cursor.isMissing() && cursor.value() instanceof String;
    }
}
