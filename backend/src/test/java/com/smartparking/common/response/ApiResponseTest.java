package com.smartparking.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successShouldUseStandardEnvelope() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ok");
    }

    @Test
    void failShouldUseResultCode() {
        ApiResponse<Void> response = ApiResponse.fail(ResultCode.UNAUTHORIZED);

        assertThat(response.code()).isEqualTo(401);
        assertThat(response.message()).isEqualTo("unauthorized");
        assertThat(response.data()).isNull();
    }
}
