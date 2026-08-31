package com.fruitmall.common;

/**
 * 用户输入清洗：在存储前剥离 HTML 标签与脚本残留，防御存储型 XSS。
 * 商品/评价/地址等用户可输入文本一律过一遍，前端再叠加 Vue 自动转义做双重防护。
 * 注：这是保守的「白名单式」清理——直接去掉所有标签，只保留纯文本，不引入额外依赖。
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // 去掉所有 HTML 标签
        String cleaned = input.replaceAll("<[^>]*>", "");
        // 去掉可能残留在文本中的脚本协议与事件处理属性
        cleaned = cleaned.replaceAll("(?i)javascript:", "");
        cleaned = cleaned.replaceAll("(?i)vbscript:", "");
        cleaned = cleaned.replaceAll("(?i)on\\w+\\s*=", "");
        // 去掉不可见控制字符（保留常规空白），避免注入畸形字符
        cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        // 压缩连续空白
        cleaned = cleaned.replaceAll("[ \\t\\r\\n]{2,}", " ").trim();
        return cleaned;
    }
}
