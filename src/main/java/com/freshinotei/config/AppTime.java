package com.freshinotei.config;

import java.time.ZoneId;

/**
 * 서비스 기준 시간대.
 * 배포 서버(Railway 등)는 UTC 라서 LocalDate.now() 를 그대로 쓰면
 * 한국 시간 00~09시 사이에 날짜가 하루 어긋난다.
 */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private AppTime() {
    }
}
