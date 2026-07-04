package com.ruoyi.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 工单 Elasticsearch 检索配置。
 */
@ConfigurationProperties(prefix = "ticket.search")
public class TicketSearchProperties {

    /** 是否启用工单检索。 */
    private boolean enabled;

    /** Elasticsearch 节点地址。 */
    private List<String> uris = new ArrayList<>(List.of("http://localhost:9200"));

    /** 工单检索固定别名。 */
    private String indexAlias = "ticket-search";

    /** 游标签名密钥，启用检索时必须由外部配置提供。 */
    private String cursorSecret;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getIndexAlias() {
        return indexAlias;
    }

    public void setIndexAlias(String indexAlias) {
        this.indexAlias = indexAlias;
    }

    public String getCursorSecret() { return cursorSecret; }
    public void setCursorSecret(String cursorSecret) { this.cursorSecret = cursorSecret; }
}
