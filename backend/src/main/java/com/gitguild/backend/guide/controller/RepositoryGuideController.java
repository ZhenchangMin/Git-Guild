package com.gitguild.backend.guide.controller;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryGuideController {

    private final CodeRepositoryRepository repositoryRepository;

    public RepositoryGuideController(CodeRepositoryRepository repositoryRepository) {
        this.repositoryRepository = repositoryRepository;
    }

    @GetMapping("/{repositoryId}/guide")
    public ApiResponse<RepositoryGuideResponse> getGuide(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "true") boolean includeFaq,
            @RequestParam(defaultValue = "true") boolean includeRecommendedQuests,
            @RequestParam(defaultValue = "zh-CN") String language) {
        CodeRepository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new BusinessException("REPOSITORY_NOT_FOUND", HttpStatus.NOT_FOUND, "仓库不存在"));
        List<GuideSection> sections = List.of(
                new GuideSection("项目结构", "先阅读仓库 README 与目录结构，确认后端、前端和脚本位置。"),
                new GuideSection("运行方式", "按照仓库说明安装依赖并启动本地开发环境。"),
                new GuideSection("贡献流程", "创建分支、完成修改、提交 Pull Request，并等待维护者审核。"));
        List<GuideFaq> faqs = includeFaq
                ? List.of(new GuideFaq("遇到运行失败怎么办？", "先检查依赖、端口、数据库和环境变量配置。"))
                : List.of();
        List<Long> recommendedQuestIds = includeRecommendedQuests ? List.of() : List.of();
        return ApiResponse.success(new RepositoryGuideResponse(
                repository.getRepositoryId(),
                repository.getName(),
                language,
                sections,
                faqs,
                recommendedQuestIds));
    }

    public record RepositoryGuideResponse(
            Long repositoryId,
            String repositoryName,
            String language,
            List<GuideSection> sections,
            List<GuideFaq> faqs,
            List<Long> recommendedQuestIds) {
    }

    public record GuideSection(String title, String content) {
    }

    public record GuideFaq(String question, String answer) {
    }
}
