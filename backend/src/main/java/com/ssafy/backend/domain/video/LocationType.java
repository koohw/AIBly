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

    public static String getDescriptionByCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .map(e -> e.description)
                .findFirst()
                .orElse("기타");
    }

    public static LocationType fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(null); // 또는 throw new IllegalArgumentException(...) 해도 됨
    }

}
