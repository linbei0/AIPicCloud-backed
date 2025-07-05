package com.aipiccloud.model.dto.picture;

import lombok.Data;

@Data
public class PictureUploadByBatchRequest {

    /**
     * 搜索词
     */
    private String searchText;
    /**
     * 名称前缀
     */
    private String namePrefix;

    /**
     * 抓取数量
     */
    private Integer count = 10;
    /**
     * 搜索源
     */
    private Integer source = 0;
    private static final long serialVersionUID = 1L;
}

