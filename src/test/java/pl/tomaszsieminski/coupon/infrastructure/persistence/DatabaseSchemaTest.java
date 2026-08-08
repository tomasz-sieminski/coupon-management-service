package pl.tomaszsieminski.coupon.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import pl.tomaszsieminski.coupon.TestcontainersConfiguration;
import pl.tomaszsieminski.coupon.infrastructure.persistence.repository.JpaCouponRepository;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseSchemaTest {

    @Autowired
    private JpaCouponRepository couponRepository;

    @Test
    @DisplayName("Should initialize Flyway migrations and validate JPA entities")
    void shouldInitializeFlywayAndValidateJpaEntities() {
        assertThat(couponRepository).isNotNull();
    }
}
