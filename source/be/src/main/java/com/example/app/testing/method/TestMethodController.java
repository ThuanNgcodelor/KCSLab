package com.example.app.testing.method;

import com.example.app.common.dto.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** GET is open to all staff (picker on the transfer form); changes are ADMIN only (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/test-methods")
public class TestMethodController {
    private final TestMethodService service;

    public TestMethodController(TestMethodService service) { this.service = service; }

    @GetMapping
    public PagedResponse<TestMethodDtos.Response> list(@RequestParam(required = false) String q,
                                                       @RequestParam(required = false) Boolean active,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @RequestParam(required = false) String sort) {
        return service.search(q, active, page, size, sort);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestMethodDtos.Response create(@Valid @RequestBody TestMethodDtos.SaveRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TestMethodDtos.Response update(@PathVariable Long id, @Valid @RequestBody TestMethodDtos.SaveRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
