package com.blog.service.impl;

import com.blog.common.CacheNames;
import com.blog.dto.SettingDTO;
import com.blog.dto.SiteInfoResponse;
import com.blog.entity.Setting;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import com.blog.repository.SettingRepository;
import com.blog.repository.TagRepository;
import com.blog.service.MarkdownRenderer;
import com.blog.service.SettingService;
import com.blog.util.SensitiveWordFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettingServiceImpl implements SettingService {

    private static final Logger log = LoggerFactory.getLogger(SettingServiceImpl.class);

    private final SettingRepository settingRepository;
    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final MarkdownRenderer markdownRenderer;
    private final SensitiveWordFilter sensitiveWordFilter;

    public SettingServiceImpl(SettingRepository settingRepository,
                              ArticleRepository articleRepository,
                              CategoryRepository categoryRepository,
                              TagRepository tagRepository,
                              MarkdownRenderer markdownRenderer,
                              SensitiveWordFilter sensitiveWordFilter) {
        this.settingRepository = settingRepository;
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.markdownRenderer = markdownRenderer;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    /**
     * 获取所有配置项
     */
    @Override
    public List<SettingDTO> getAllSettings() {
        log.debug("获取所有配置项");
        return settingRepository.findAll().stream()
                .map(s -> new SettingDTO(s.getId(), s.getSettingKey(), s.getSettingValue(), s.getDescription()))
                .toList();
    }

    /**
     * 批量更新配置项（逐个更新已有配置或新增）
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.SITE_INFO, allEntries = true)
    public void updateSettings(List<SettingDTO> settings) {
        log.info("批量更新配置项 count={}", settings.size());
        for (SettingDTO dto : settings) {
            /* 查找已有配置，不存在则新建 */
            Setting setting = settingRepository.findBySettingKey(dto.settingKey())
                    .orElseGet(() -> {
                        Setting newSetting = Setting.builder()
                                .settingKey(dto.settingKey())
                                .build();
                        return newSetting;
                    });
            setting.setSettingValue(dto.settingValue());
            if (dto.description() != null) {
                setting.setDescription(dto.description());
            }
            settingRepository.save(setting);
        }
        /* 如果更新了敏感词配置，立即刷新过滤器缓存 */
        if (settings.stream().anyMatch(dto -> "sensitive_words".equals(dto.settingKey()))) {
            sensitiveWordFilter.refresh();
        }
        log.info("配置更新完成");
    }

    /**
     * C端：获取站点信息（含文章/分类/标签数量、about 内容）
     */
    @Override
    @Cacheable(value = CacheNames.SITE_INFO)
    public SiteInfoResponse getSiteInfo() {
        log.debug("C端获取站点信息");
        List<Setting> allSettings = settingRepository.findAll();
        /* 将配置列表转为 Map，方便取值 */
        Map<String, String> settingsMap = allSettings.stream()
                .collect(Collectors.toMap(Setting::getSettingKey, s -> {
                    String val = s.getSettingValue();
                    return val != null ? val : "";
                }));

        String siteTitle = settingsMap.getOrDefault("site_title", "Blog");
        String siteDescription = settingsMap.getOrDefault("site_description", "");
        /* 渲染 about 内容为 HTML */
        String aboutContent = markdownRenderer.render(settingsMap.getOrDefault("about_content", ""));

        /* 从数据库获取实时数量 */
        long articleCount = articleRepository.countByStatus("PUBLISHED");
        long categoryCount = categoryRepository.count();
        long tagCount = tagRepository.count();

        return new SiteInfoResponse(siteTitle, siteDescription,
                articleCount, categoryCount, tagCount, aboutContent);
    }
}
