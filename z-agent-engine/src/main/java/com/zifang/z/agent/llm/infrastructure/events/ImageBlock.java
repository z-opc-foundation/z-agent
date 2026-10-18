package com.zifang.z.agent.llm.infrastructure.events;

/**
 * Image block — a content block representing an image URL.
 */
public class ImageBlock extends ContentBlock {

    private final String url;
    private final String description;

    public ImageBlock(String url, String description) {
        super("image");
        this.url = url;
        this.description = description;
    }

    public static ImageBlock url(String url, String description) {
        return new ImageBlock(url, description);
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }
}
