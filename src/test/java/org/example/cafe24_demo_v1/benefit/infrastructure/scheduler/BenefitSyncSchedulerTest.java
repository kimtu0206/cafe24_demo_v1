package org.example.cafe24_demo_v1.benefit.infrastructure.scheduler;

import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BenefitSyncSchedulerTest {

    @Mock private BenefitService benefitService;
    @Mock private Cafe24Properties cafe24Properties;

    private BenefitSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BenefitSyncScheduler(benefitService, cafe24Properties);
    }

    @Test
    void syncBenefits는_mallId를_이용해_syncFromCafe24를_호출한다() {
        given(cafe24Properties.getMallId()).willReturn("mymall");

        scheduler.syncBenefits();

        verify(benefitService).syncFromCafe24("mymall");
    }
}
