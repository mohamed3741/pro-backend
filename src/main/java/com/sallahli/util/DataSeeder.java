package com.sallahli.util;

import com.github.javafaker.Faker;
import com.sallahli.model.*;
import com.sallahli.model.Enum.KycStatus;
import com.sallahli.model.Enum.LeadType;
import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProRepository proRepository;
    private final ClientRepository clientRepository;
    private final MediaRepository mediaRepository;
    private final ZoneRepository zoneRepository;
    private final JobRepository jobRepository;
    private final CustomerRequestRepository customerRequestRepository;

    private final Faker faker = new Faker(new Locale("en"));

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (categoryRepository.count() > 0) {
            log.info("DataSeeder: Data already exists, skipping seeding.");
            return;
        }

        log.info("DataSeeder: Starting data seeding...");

        // 1. Seed Media
        Media plumbingIcon = createMedia("https://placehold.co/100x100/png?text=Plumbing",
                "Plumbing Icon");
        Media electricIcon = createMedia("https://placehold.co/100x100/png?text=Electric",
                "Electricity Icon");
        Media cleaningIcon = createMedia("https://placehold.co/100x100/png?text=Cleaning",
                "Cleaning Icon");
        Media profilePic = createMedia("https://placehold.co/200x200/png?text=Profile", "Profile Pic");

        // 2. Seed Zones
        Zone zone = Zone.builder()
                .name("Nouakchott")
                .city("Nouakchott")
                .country("Mauritania")
                .active(true)
                .build();
        zone = zoneRepository.save(zone);

        // 3. Seed Categories
        List<Category> categories = new ArrayList<>();
        categories.add(createCategory("PLUMBING", "Plumbing", "All plumbing services", plumbingIcon));
        categories.add(
                createCategory("ELECTRICITY", "Electricity", "Electrical repairs and installations", electricIcon));
        categories.add(createCategory("CLEANING", "Cleaning", "Home and office cleaning", cleaningIcon));

        // 4. Seed Pros
        for (int i = 0; i < 20; i++) {
            createPro(categories.get(faker.random().nextInt(categories.size())), zone, profilePic);
        }

        // 5. Seed Clients
        for (int i = 0; i < 10; i++) {
            createClient(profilePic);
        }

        log.info("DataSeeder: Data seeding completed.");
    }

    private Media createMedia(String link, String keyName) {
        return mediaRepository.save(Media.builder()
                .type(MediaEnum.IMAGE)
                .link(link)
                .thumbnail(link)
                .keyName(keyName)
                .mimeType("image/png")
                .sizeBytes(1024L)
                .build());
    }

    private Category createCategory(String code, String name, String description, Media icon) {
        return categoryRepository.save(Category.builder()
                .code(code)
                .name(name)
                .description(description)
                .iconMedia(icon)
                .leadType(LeadType.FIXED)
                .leadCost(BigDecimal.valueOf(50))
                .matchLimit(5)
                .workflowType(WorkflowType.LEAD_OFFER)
                .active(true)
                .build());
    }

    private void createPro(Category trade, Zone zone, Media profilePhoto) {
        proRepository.save(Pro.builder()
                .tel(faker.phoneNumber().cellPhone())
                .username(faker.name().username())
                .firstName(faker.name().firstName())
                .lastName(faker.name().lastName())
                .fullName(faker.name().fullName())
                .email(faker.internet().emailAddress())
                .trade(trade)
                .baseZone(zone)
                .kycStatus(KycStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .online(true)
                .ratingAvg(4.5 + faker.random().nextDouble() * 0.5)
                .ratingCount((long) faker.number().numberBetween(10, 100))
                .jobsCompleted((long) faker.number().numberBetween(5, 50))
                .walletBalance(1000L)
                .isActive(true)
                .build());
    }

    private void createClient(Media logo) {
        clientRepository.save(Client.builder()
                .tel(faker.phoneNumber().cellPhone())
                .firstName(faker.name().firstName())
                .lastName(faker.name().lastName())
                .email(faker.internet().emailAddress())
                .logo(logo)
                .wallet(500)
                .isActive(true)
                .isTelVerified(true)
                .build());
    }
}
