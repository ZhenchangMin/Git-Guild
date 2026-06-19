package com.gitguild.backend.quest.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.domain.QuestTag;
import com.gitguild.backend.quest.domain.QuestTechStack;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.repository.QuestTagRepository;
import com.gitguild.backend.quest.repository.QuestTechStackRepository;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    /** 已下架/已驳回的 Quest 不计入分类/标签的「占用」判定，参见 QuestRepository 的查询注释。 */
    private static final Set<QuestStatus> INACTIVE_STATUSES = EnumSet.of(QuestStatus.CLOSED, QuestStatus.REJECTED);

    private final QuestCategoryRepository categoryRepository;
    private final QuestTagRepository tagRepository;
    private final QuestTechStackRepository techStackRepository;
    private final QuestRepository questRepository;

    public QuestTaxonomyController(
            QuestCategoryRepository categoryRepository,
            QuestTagRepository tagRepository,
            QuestTechStackRepository techStackRepository,
            QuestRepository questRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.techStackRepository = techStackRepository;
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
                        withQuestCount ? (int) questRepository.countByCategory_CategoryIdAndStatusNotIn(category.getCategoryId(), INACTIVE_STATUSES) : null))
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
                CategoryResponse.from(saved, (int) questRepository.countByCategory_CategoryIdAndStatusNotIn(saved.getCategoryId(), INACTIVE_STATUSES)));
    }

    @DeleteMapping("/quest-categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        QuestCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> notFound("CATEGORY_NOT_FOUND", "任务分类不存在"));
        long used = questRepository.countByCategory_CategoryIdAndStatusNotIn(categoryId, INACTIVE_STATUSES);
        if (used > 0) {
            throw new BusinessException(
                    "CATEGORY_IN_USE", HttpStatus.CONFLICT, "分类正被任务引用，无法删除", "questCount=" + used);
        }
        categoryRepository.delete(category);
        return ApiResponse.success();
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
                .map(tag -> TagResponse.from(tag, (int) questRepository.countByTagIdAndStatusNotIn(tag.getTagId(), INACTIVE_STATUSES)))
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
        return ApiResponse.success(TagResponse.from(saved, (int) questRepository.countByTagIdAndStatusNotIn(saved.getTagId(), INACTIVE_STATUSES)));
    }

    @DeleteMapping("/quest-tags/{tagId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTag(@PathVariable Long tagId) {
        QuestTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> notFound("TAG_NOT_FOUND", "任务标签不存在"));
        long used = questRepository.countByTagIdAndStatusNotIn(tagId, INACTIVE_STATUSES);
        if (used > 0) {
            throw new BusinessException(
                    "TAG_IN_USE", HttpStatus.CONFLICT, "标签正被任务引用，无法删除", "questCount=" + used);
        }
        tagRepository.delete(tag);
        return ApiResponse.success();
    }

    @GetMapping("/quest-tech-stacks")
    public ApiResponse<TechStackPageResponse> listTechStacks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeDisabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        List<TechStackResponse> filtered = techStackRepository.findAll().stream()
                .filter(stack -> includeDisabled || stack.isEnabled())
                .filter(stack -> keyword == null || keyword.isBlank() || stack.getName().contains(keyword.trim()))
                .sorted(Comparator.comparing(QuestTechStack::getName))
                .map(stack -> TechStackResponse.from(
                        stack, (int) questRepository.countByTechStackNameAndStatusNotIn(stack.getName(), INACTIVE_STATUSES)))
                .toList();
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return ApiResponse.success(new TechStackPageResponse(filtered.subList(from, to), page, size, filtered.size()));
    }

    @PostMapping("/quest-tech-stacks")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TechStackResponse> createTechStack(@RequestBody TechStackRequest request) {
        validateName(request.name());
        if (techStackRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw alreadyExists("同名技术栈已存在");
        }
        QuestTechStack stack = techStackRepository.save(new QuestTechStack(request.name().trim()));
        return ApiResponse.success("CREATED", TechStackResponse.from(stack, 0));
    }

    @PatchMapping("/quest-tech-stacks/{techStackId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TechStackResponse> updateTechStack(
            @PathVariable Long techStackId,
            @RequestBody TechStackRequest request) {
        QuestTechStack stack = techStackRepository.findById(techStackId)
                .orElseThrow(() -> notFound("TECH_STACK_NOT_FOUND", "技术栈不存在"));
        stack.update(request.name(), request.enabled());
        QuestTechStack saved = techStackRepository.save(stack);
        return ApiResponse.success(
                TechStackResponse.from(saved, (int) questRepository.countByTechStackNameAndStatusNotIn(saved.getName(), INACTIVE_STATUSES)));
    }

    @DeleteMapping("/quest-tech-stacks/{techStackId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteTechStack(@PathVariable Long techStackId) {
        QuestTechStack stack = techStackRepository.findById(techStackId)
                .orElseThrow(() -> notFound("TECH_STACK_NOT_FOUND", "技术栈不存在"));
        long used = questRepository.countByTechStackNameAndStatusNotIn(stack.getName(), INACTIVE_STATUSES);
        if (used > 0) {
            throw new BusinessException(
                    "TECH_STACK_IN_USE", HttpStatus.CONFLICT, "技术栈正被任务引用，无法删除", "questCount=" + used);
        }
        techStackRepository.delete(stack);
        return ApiResponse.success();
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

    public record TechStackRequest(String name, Boolean enabled) {
    }

    public record TechStackResponse(Long techStackId, String name, Boolean enabled, Integer questCount) {
        static TechStackResponse from(QuestTechStack stack, Integer questCount) {
            return new TechStackResponse(stack.getTechStackId(), stack.getName(), stack.isEnabled(), questCount);
        }
    }

    public record TechStackPageResponse(List<TechStackResponse> items, int page, int size, int total) {
    }
}
