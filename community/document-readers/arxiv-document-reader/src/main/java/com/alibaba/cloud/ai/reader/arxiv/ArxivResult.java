package com.alibaba.cloud.ai.reader.arxiv;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * arXiv查询结果中的一个条目
 * 
 * @see <a href="https://arxiv.org/help/api/user-manual#_details_of_atom_results_returned">arXiv API User's Manual: Details of Atom Results Returned</a>
 */
public class ArxivResult {
    
    private String entryId;                  // 形如 https://arxiv.org/abs/{id} 的URL
    private LocalDateTime updated;           // 最后更新时间
    private LocalDateTime published;         // 最初发布时间
    private String title;                    // 标题
    private List<ArxivAuthor> authors;       // 作者列表
    private String summary;                  // 摘要
    private String comment;                  // 作者评论(可选)
    private String journalRef;               // 期刊引用(可选)
    private String doi;                      // DOI链接(可选)
    private String primaryCategory;          // 主要分类
    private List<String> categories;         // 所有分类
    private List<ArxivLink> links;           // 相关链接(最多3个)
    private String pdfUrl;                   // PDF链接(如果存在)

    // Getters and Setters
    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }
    
    public LocalDateTime getUpdated() { return updated; }
    public void setUpdated(LocalDateTime updated) { this.updated = updated; }
    
    public LocalDateTime getPublished() { return published; }
    public void setPublished(LocalDateTime published) { this.published = published; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public List<ArxivAuthor> getAuthors() { return authors; }
    public void setAuthors(List<ArxivAuthor> authors) { this.authors = authors; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public String getJournalRef() { return journalRef; }
    public void setJournalRef(String journalRef) { this.journalRef = journalRef; }
    
    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }
    
    public String getPrimaryCategory() { return primaryCategory; }
    public void setPrimaryCategory(String primaryCategory) { this.primaryCategory = primaryCategory; }
    
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    
    public List<ArxivLink> getLinks() { return links; }
    public void setLinks(List<ArxivLink> links) { 
        this.links = links;
        // 设置PDF URL
        this.pdfUrl = links.stream()
                .filter(link -> "pdf".equals(link.getTitle()))
                .findFirst()
                .map(ArxivLink::getHref)
                .orElse(null);
    }
    
    public String getPdfUrl() { return pdfUrl; }

    /**
     * 获取文章的短ID
     * 例如:
     * - URL为"https://arxiv.org/abs/2107.05580v1"时返回"2107.05580v1"
     * - URL为"https://arxiv.org/abs/quant-ph/0201082v1"时返回"quant-ph/0201082v1"
     */
    public String getShortId() {
        return entryId.split("arxiv.org/abs/")[1];
    }

    /**
     * 生成默认的文件名
     */
    public String getDefaultFilename(String extension) {
        String nonEmptyTitle = title != null && !title.isEmpty() ? title : "UNTITLED";
        return String.format("%s.%s.%s",
                getShortId().replace("/", "_"),
                Pattern.compile("[^\\w]").matcher(nonEmptyTitle).replaceAll("_"),
                extension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArxivResult result = (ArxivResult) o;
        return Objects.equals(entryId, result.entryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryId);
    }

    /**
     * 表示文章作者的内部类
     */
    public static class ArxivAuthor {
        private String name;

        public ArxivAuthor(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArxivAuthor author = (ArxivAuthor) o;
            return Objects.equals(name, author.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * 表示相关链接的内部类
     */
    public static class ArxivLink {
        private String href;           // 链接URL
        private String title;          // 链接标题
        private String rel;            // 链接与Result的关系
        private String contentType;    // HTTP内容类型

        public ArxivLink(String href, String title, String rel, String contentType) {
            this.href = href;
            this.title = title;
            this.rel = rel;
            this.contentType = contentType;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getRel() {
            return rel;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArxivLink link = (ArxivLink) o;
            return Objects.equals(href, link.href);
        }

        @Override
        public int hashCode() {
            return Objects.hash(href);
        }

        @Override
        public String toString() {
            return href;
        }
    }
} 