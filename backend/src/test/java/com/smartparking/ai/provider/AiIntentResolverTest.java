package com.smartparking.ai.provider;

import com.smartparking.ai.enums.AiIntent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiIntentResolverTest {

    private final AiIntentResolver resolver = new AiIntentResolver();

    @Test
    void shouldResolveGreeting() {
        assertThat(resolver.resolve("你好")).isEqualTo(AiIntent.GREETING);
    }

    @Test
    void shouldResolveSpaceStatus() {
        assertThat(resolver.resolve("还有多少空闲车位？")).isEqualTo(AiIntent.SPACE_STATUS);
    }

    @Test
    void shouldResolveFeeRuleBeforeEntryExitGuide() {
        assertThat(resolver.resolve("停车费用怎么算？")).isEqualTo(AiIntent.FEE_RULE);
    }

    @Test
    void shouldResolveMyVehicles() {
        assertThat(resolver.resolve("我登记了哪些车辆？")).isEqualTo(AiIntent.MY_VEHICLES);
    }

    @Test
    void shouldResolveMyReservations() {
        assertThat(resolver.resolve("我的预约有哪些？")).isEqualTo(AiIntent.MY_RESERVATIONS);
    }

    @Test
    void shouldResolveReservationGuide() {
        assertThat(resolver.resolve("怎么在线预约车位？")).isEqualTo(AiIntent.RESERVATION_GUIDE);
    }

    @Test
    void shouldResolveEntryExitGuide() {
        assertThat(resolver.resolve("车辆入场出场流程是什么？")).isEqualTo(AiIntent.ENTRY_EXIT_GUIDE);
    }

    @Test
    void shouldResolveSuggestionGuide() {
        assertThat(resolver.resolve("我想提一个建议")).isEqualTo(AiIntent.SUGGESTION_GUIDE);
    }

    @Test
    void shouldResolveParkingRecords() {
        assertThat(resolver.resolve("我的停车记录")).isEqualTo(AiIntent.MY_PARKING_RECORDS);
    }

    @Test
    void shouldFallbackToUnknown() {
        assertThat(resolver.resolve("今天天气怎么样")).isEqualTo(AiIntent.UNKNOWN);
    }

    @Test
    void shouldFallbackWhenQuestionIsBlank() {
        assertThat(resolver.resolve("   ")).isEqualTo(AiIntent.UNKNOWN);
    }
}
