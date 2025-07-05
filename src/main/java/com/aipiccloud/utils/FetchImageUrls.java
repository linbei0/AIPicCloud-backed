package com.aipiccloud.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aipiccloud.exception.BusinessException;
import com.aipiccloud.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
public class FetchImageUrls {
    /**
     * Bing搜索源实现
     */
    public static List<String> FromBing(String searchText) {
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        try {
            Document document = Jsoup.connect(fetchUrl).get();
            Element div = document.getElementsByClass("dgControl").first();
            if (ObjUtil.isNull(div)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
            }
            return div.select("img.mimg").stream()
                    .map(img -> img.attr("src"))
                    .filter(url -> StrUtil.isNotBlank(url))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
    }

    /**
     * Pexels搜索源实现
     * @param searchText
     * @param count
     * @return
     */

    public static List<String> FormPexels(String searchText, int count,String apiKey) {
        // 参数校验
        if (StrUtil.isBlank(searchText) || count <= 0) {
            return List.of();
        }

        try {
            // 构建带参数的请求URL
            String requestUrl = String.format("%s?query=%s&per_page=%d",
                    "https://api.pexels.com/v1/search",
                    URLUtil.encode(searchText),
                    Math.min(count, 80));  // 限制最大每页数量

            // 发送带认证头的GET请求
            HttpResponse response = HttpRequest.get(requestUrl)
                    .header(Header.AUTHORIZATION, apiKey)
                    .timeout(5000)
                    .execute();

            // 响应状态校验
            if (response.getStatus() != 200) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Pexels API 请求失败");
            }

            // 解析JSON响应
            JSONObject jsonObject = JSONUtil.parseObj(response.body());
            JSONArray photosArray = jsonObject.getJSONArray("photos");

            // 提取并过滤图片URL
            return JSONUtil.toList(photosArray, JSONObject.class).stream()
                    .map(photo -> {
                        JSONObject srcObj = photo.getJSONObject("src");
                        return srcObj != null ? srcObj.getStr("original") : null;
                    })
                    .filter(url -> StrUtil.isNotBlank(url))
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pexels图片搜索异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片搜索失败");
        }
    }
}
