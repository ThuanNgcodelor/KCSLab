package com.example.app.testing.method;

import com.example.app.common.dto.PagedResponse;
import com.example.app.common.query.ListQuery;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@Service
public class TestMethodService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code", "createdAt", "createdAt", "updatedAt", "updatedAt", "active", "active");

    private final TestMethodRepository repository;

    public TestMethodService(TestMethodRepository repository) { this.repository = repository; }

    /** Paged search on code and name; the picker asks for active entries only. */
    @Transactional(readOnly = true)
    public PagedResponse<TestMethodDtos.Response> search(String q, Boolean active, int page, int size, String sort) {
        var parsed = ListQuery.parseSort(sort, SORT_FIELDS, "code,asc");
        Specification<TestMethod> spec = (root, query, builder) -> builder.conjunction();
        if (active != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("active"), active));
        if (ListQuery.hasText(q)) {
            String pattern = ListQuery.contains(q);
            spec = spec.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("code")), pattern, '\\'),
                    builder.like(builder.lower(builder.coalesce(root.get("name"), "")), pattern, '\\')));
        }
        var result = repository.findAll(spec, ListQuery.page(page, size, parsed)).map(TestMethodDtos.Response::from);
        return PagedResponse.from(result, parsed.canonical());
    }

    @Transactional
    public TestMethodDtos.Response create(TestMethodDtos.SaveRequest request) {
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCase(code)) throw duplicate(code);
        TestMethod method = new TestMethod(code, blankToNull(request.name()), request.active() == null || request.active());
        return TestMethodDtos.Response.from(saveAndFlush(method, code));
    }

    @Transactional
    public TestMethodDtos.Response update(Long id, TestMethodDtos.SaveRequest request) {
        TestMethod method = get(id);
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) throw duplicate(code);
        method.update(code, blankToNull(request.name()), request.active() == null ? method.isActive() : request.active());
        return TestMethodDtos.Response.from(saveAndFlush(method, code));
    }

    /** Transfers keep the method code as text, so deleting a catalog entry never changes an existing form. */
    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private TestMethod saveAndFlush(TestMethod method, String code) {
        try {
            return repository.saveAndFlush(method);
        } catch (DataIntegrityViolationException exception) {
            throw duplicate(code);
        }
    }

    private TestMethod get(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phương pháp thử"));
    }

    private static String normalizeCode(String code) {
        return code.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException duplicate(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Mã phương pháp " + code + " đã có trong danh mục");
    }
}
