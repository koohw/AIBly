package com.ssafy.backend.report.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class AccidentInfoResolver {

    private final Map<Integer, AccidentInfo> accidentTypeMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/static/case_data.csv")),
                StandardCharsets.UTF_8
        ))) {
            reader.readLine(); // 헤더 스킵

            reader.lines().forEach(line -> {
                String[] parts = line.split(",");
                try {
                    int accidentType = Integer.parseInt(parts[7].trim());

                    AccidentInfo info = new AccidentInfo(
                            parts[0], // 사고 번호
                            parts[1], // 사고 장소
                            parts[2], // 사고 장소 특징
                            parts[3], // A 진행 방향
                            parts[4], // B 진행 방향
                            parts[5], // 과실 비율 A
                            parts[6], // 과실 비율 B
                            accidentType,
                            parts[8], // 차번호/사고유형
                            parts[9], // 관련 법규
                            parts[10] // 판례·조정사례
                    );

                    accidentTypeMap.put(accidentType, info);

                } catch (Exception e) {
                    System.err.println("CSV 파싱 오류: " + Arrays.toString(parts));
                }
            });

        } catch (Exception e) {
            throw new RuntimeException("CSV 파일 로딩 실패", e);
        }
    }

    public AccidentInfo resolve(int accidentType) {
        return accidentTypeMap.getOrDefault(
                accidentType,
                new AccidentInfo("N/A", "", "", "", "", "", "", accidentType, "설명 없음", "", "")
        );
    }

    @Getter
    public static class AccidentInfo {
        private final String accidentId;
        private final String accidentPlace;
        private final String accidentPlaceFeature;
        private final String vehicleAProgress;
        private final String vehicleBProgress;
        private final String carAFaultRatio;
        private final String carBFaultRatio;
        private final int accidentType;
        private final String accidentDescription;
        private final String laws;
        private final String precedents;

        public AccidentInfo(String accidentId, String accidentPlace, String accidentPlaceFeature,
                            String vehicleAProgress, String vehicleBProgress,
                            String carAFaultRatio, String carBFaultRatio,
                            int accidentType, String accidentDescription,
                            String laws, String precedents) {
            this.accidentId = accidentId;
            this.accidentPlace = accidentPlace;
            this.accidentPlaceFeature = accidentPlaceFeature;
            this.vehicleAProgress = vehicleAProgress;
            this.vehicleBProgress = vehicleBProgress;
            this.carAFaultRatio = carAFaultRatio;
            this.carBFaultRatio = carBFaultRatio;
            this.accidentType = accidentType;
            this.accidentDescription = accidentDescription;
            this.laws = laws;
            this.precedents = precedents;
        }
    }
}

/*
CSV에 맞춘 매핑 로직.

CSV 기반 사고 유형 및 판례 데이터를 메모리에 올려놓고,
JSON에서 받은 정보에 기반해 사고유형(accident_type이 기준)에 맞는 정보를 찾아준다.

일단 지금은 csv에 있는 11개 값 다 넣어놓음.

Report에서 필요한 값은 AccidentInfo에서 반드시 제공해야함.

AccidentInfoResolver는 CSV에서 데이터를 읽고,
그 중 사고 유형(accident_type)에 해당하는 행을 찾아서 반환하는 유틸이고,
그 결과로 돌려주는 구조체가 AccidentInfo
*/