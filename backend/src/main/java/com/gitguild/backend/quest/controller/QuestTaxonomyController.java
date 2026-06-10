package com.gitguild.backend.quest.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestTag;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.repository.QuestTagRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QuestTaxonomyController {

    private final QuestCategoryRepository categoryRepository;
    private final QuestTagRepository tagRepository;
    private final QuestRepository questRepository;

    public QuestTaxonomyController(
            QuestCategoryRepository categoryRepository,
            QuestTagRepository tagRepository,
            QuestRepository questRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.questRepository = questRepository;
    }

    @GetMapping("/quest-categories")
    public ApiResponse<List<CategoryResponse>> listCategories(
            @RequestParam(defaultValue = "false") boolean withQuestCount,
            @RequestParam(defaultValue = "false") boolean includeDisabled,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        List<CategoryResponse> items = categoryRepository.findAll().stream()
                .filter(category -> includeDisabled || category.isEnabled())
                .map(category -> CategoryResponse.from(
                        category,
                        withQuestCount ? (int) questRepository.countByCategory_CategoryId(category.getCategoryId()) : null))
                .sorted(categoryComparator(sortBy, sortOrder))
                .toList();
        return ApiResponse.success(items);
    }

    @PostMapping("/quest-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        validateName(request.name());
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw alreadyExists("同名分类已存在");
        }
        QuestCategory category = categoryRepository.save(new QuestCategory(request.name().trim(), request.description()));
        return ApiResponse.success("CREATED", CategoryResponse.from(category, null));
    }

    @PatchMapping("/quest-categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody CategoryRequest request) {
        QuestCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> notFound("CATEGORY_NOT_FOUND", "任务分类不存在"));
        category.update(request.name(), request.description(), request.enabled());
        QuestCategory saved = categoryRepository.save(category);
        return ApiResponse.success(
                CategoryResponse.from(saved, (int) questRepository.countByCategory_CategoryId(saved.getCategoryId())));
    }

    @GetMapping("/quest-tags")
    public ApiResponse<TagPageResponse> listTags(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeDisabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        List<TagResponse> filtered = tagRepository.findAll().stream()
                .filter(tag -> includeDisabled || tag.isEnabled())
                .filter(tag -> keyword == null || keyword.isBlank() || tag.getName().contains(keyword.trim()))
                .sorted(Comparator.comparing(QuestTag::getName))
                .map(tag -> TagResponse.from(tag, (int) questRepository.countByTagId(tag.getTagId())))
                .toList();
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return ApiResponse.success(new TagPageResponse(filtered.subList(from, to), page, size, filtered.size()));
    }

    @PostMapping("/quest-tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> createTag(@RequestBody TagRequest request) {
        validateName(request.name());
        if (tagRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw alreadyExists("同名标签已存在");
        }
        QuestTag tag = tagRepository.save(new QuestTag(request.name().trim(), request.color()));
        return ApiResponse.success("CREATED", TagResponse.from(tag, 0));
    }

    @PatchMapping("/quest-tags/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> updateTag(@PathVariable Long tagId, @RequestBody TagRequest request) {
        QuestTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> notFound("TAG_NOT_FOUND", "任务标签不存在"));
        tag.update(request.name(), request.color(), request.enabled());
        QuestTag saved = tagRepository.save(tag);
        return ApiResponse.success(TagResponse.from(saved, (int) questRepository.countByTagId(saved.getTagId())));
    }

    private Comparator<CategoryResponse> categoryComparator(String sortBy, String sortOrder) {
        Comparator<CategoryResponse> comparator = "questCount".equals(sortBy)
                ? Comparator.comparing(response -> response.questCount() == null ? 0 : response.questCount())
                : Comparator.comparing(CategoryResponse::name);
        return "desc".equalsIgnoreCase(sortOrder) ? comparator.reversed() : comparator;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", "name is required");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(
                    "VALIDATION_FAILED",
                    HttpStatus.BAD_REQUEST,
                    "请求参数不合法",
                    "page must be >= 1 and size must be between 1 and 100");
        }
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(code, HttpStatus.NOT_FOUND, message);
    }

    private BusinessException alreadyExists(String message) {
        return new BusinessException("ALREADY_EXISTS", HttpStatus.CONFLICT, message);
    }

    public record CategoryRequest(String name, String description, Boolean enabled) {
    }

    public record CategoryResponse(
            Long categoryId, String name, String description, Boolean enabled, Integer questCount) {
        static CategoryResponse from(QuestCategory category, Integer questCount) {
            return new CategoryResponse(
                    category.getCategoryId(),
                    category.getName(),
                    category.getDescription(),
                    category.isEnabled(),
                    questCount);
        }
    }

    public record TagRequest(String name, String color, Boolean enabled) {
    }

    public record TagResponse(Long tagId, String name, String color, Boolean enabled, Integer questCount) {
        static TagResponse from(QuestTag tag, Integer questCount) {
            return new TagResponse(tag.getTagId(), tag.getName(), tag.getColor(), tag.isEnabled(), questCount);
        }
    }

    public record TagPageResponse(List<TagResponse> items, int page, int size, int total) {
    }
}
