package com.ssafy.backend.domain.video;

import java.util.Arrays;

public enum LocationType {
    STRAIGHT_ROAD(1, "직선도로"),
    T_INTERSECTION(2, "T자형교차로"),
    PARKING_LOT(3, "주차장");

    private final int code;
    private final String description;

    LocationType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return description;
    }


    //안전한 description 조회 메서드 (기본값 포함)
    public static String getDescriptionByCode(int code) {
        return fromCode(code).getDescription(); // 아래에서 기본값 처리하므로 안전
    }

    //일단 기본으로 2번으로 기본값 설정함 (e.g. T_INTERSECTION)
    public static LocationType fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(T_INTERSECTION); // 기본값으로 2번 T자형 교차로 사용
    }
}
