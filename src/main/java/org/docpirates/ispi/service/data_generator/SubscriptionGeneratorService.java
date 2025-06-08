package org.docpirates.ispi.service.data_generator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.docpirates.ispi.entity.Subscription;
import org.docpirates.ispi.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionGeneratorService {

    private final SubscriptionRepository subscriptionRepository;

    @PostConstruct
    public void generateSubscriptions() {
        if (subscriptionRepository.count() > 0) return;

        List<Subscription> subscriptions = List.of(
                Subscription.builder()
                        .name("Меценат")
                        .price(BigDecimal.ZERO)
                        .description("Якщо людина завантажує 10 або більше документів, їй надається право на безкоштовне завантаження/друк 10 документів на день протягом тижня")
                        .build(),

                Subscription.builder()
                        .name("Бібліотекар")
                        .price(new BigDecimal("10.00"))
                        .description("Платна підписка, що надає можливість завантажувати файли у необмеженій кількості")
                        .build(),

                Subscription.builder()
                        .name("Захист+")
                        .price(new BigDecimal("20.00"))
                        .description("Платна підписка, що надає можливість переглядати файли без реклами та завантажувати їх у необмеженій кількості. А також гарантує неможливість завантаження купленої у нас роботи користувача у загальну базу")
                        .build()
        );

        subscriptionRepository.saveAll(subscriptions);
    }
}
