package com.example.app.testing.method;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TestMethodServiceTest {
    @Autowired private TestMethodService service;

    @Test
    void seededMethodsAreSearchableAndCodesStayUnique() {
        var seeded = service.search("tn-16", true, 0, 20, null);
        assertThat(seeded.content()).extracting(TestMethodDtos.Response::code).containsExactly("CFC-TN-16-2026");

        var created = service.create(new TestMethodDtos.SaveRequest("  cfc-tn-16-2027 ", "Auramine O", null));
        assertThat(created.code()).isEqualTo("CFC-TN-16-2027");
        assertThat(created.active()).isTrue();
        assertThat(service.search("auramine", null, 0, 20, null).content()).extracting(TestMethodDtos.Response::id)
                .containsExactly(created.id());

        assertThatThrownBy(() -> service.create(new TestMethodDtos.SaveRequest("CFC-TN-16-2027", null, true)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("đã có trong danh mục");

        var renamed = service.update(created.id(), new TestMethodDtos.SaveRequest("CFC-TN-16-2028", "Auramine O", false));
        assertThat(renamed.code()).isEqualTo("CFC-TN-16-2028");
        assertThat(service.search("2028", true, 0, 20, null).content()).isEmpty();

        service.delete(created.id());
        assertThat(service.search("2028", null, 0, 20, null).content()).isEmpty();
    }
}
