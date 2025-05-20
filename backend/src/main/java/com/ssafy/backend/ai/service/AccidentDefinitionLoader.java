package com.ssafy.backend.ai.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.ssafy.backend.ai.dto.response.AccidentDefinitionDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//CSV 파일을 읽고 Map<Integer, AccidentDefinition>으로 매핑
@Slf4j
@Component
public class AccidentDefinitionLoader {

    private final Map<Integer, AccidentDefinitionDto> accidentMap = new HashMap<>();

    @PostConstruct
    public void load() {
        log.info("AccidentDefinitionLoader.load() 시작됨");
        try (
                InputStream in = getClass().getResourceAsStream("/static/case_data.csv");
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                CSVReader csvReader = new CSVReader(reader)
        ) {
            log.info("✅ AccidentDefinitionLoader Bean 초기화됨");
            List<String[]> lines = csvReader.readAll();
            String[] header = lines.get(0);
            lines.remove(0);

            for (String[] parts : lines) {
                try {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < header.length; i++) {
                        row.put(header[i].trim(), parts.length > i ? parts[i].trim() : "null");
                    }

                    //log.info("현재 사고 유형: {}", row.get("사고 유형"));
                    //log.info("과실 A: {}, 과실 B: {}", row.get("과실 비율 A"), row.get("과실 비율 B"));

                    int code = Integer.parseInt(row.get("사고 유형"));
                    AccidentDefinitionDto def = new AccidentDefinitionDto(
                            row.get("사고 장소"),
                            row.get("사고 장소 특징"),
                            row.get("A 진행 방향"),
                            row.get("B 진행 방향"),
                            parseSafeInt(row.get("과실 비율 A")),
                            parseSafeInt(row.get("과실 비율 B")),
                            row.get("차번호/사고유형"),
                            row.get("관련법규"),
                            row.get("판례·조정사례")
                    );
                    accidentMap.put(code, def);
                } catch (Exception e) {
                    log.warn("CSV 한 줄 파싱 실패: {}", Arrays.toString(parts), e);
                }
            }
            //log.info("최종 등록된 accidentMap 키 목록: {}", accidentMap.keySet());
            for (Map.Entry<Integer, AccidentDefinitionDto> entry : accidentMap.entrySet()) {
                //log.info("📌 사고 코드 {} → faultA={}, faultB={}", entry.getKey(), entry.getValue().getFaultA(), entry.getValue().getFaultB());
            }

        } catch (IOException | CsvException e) {
            throw new RuntimeException("CSV 파일 로딩 실패", e);
        }
    }

    private int parseSafeInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            log.warn("⚠️ 숫자 파싱 실패: '{}'", value);
            return 0;
        }
    }



    // ✅ 반드시 public
    public AccidentDefinitionDto get(int code) {
        if (!accidentMap.containsKey(code)) {
            log.warn("❌ AccidentDefinitionLoader: 존재하지 않는 코드 {}", code);
        } else {
            log.info("✅ AccidentDefinitionLoader: 정상 조회된 코드 {}", code);
        }
        return accidentMap.getOrDefault(code, AccidentDefinitionDto.builder()
                .accidentPlace("기타") //CSV에 사고 유형 번호가 존재하지 않거나 매핑이 안 되었을 때 반환되는 기본값
                .accidentFeature("기타")
                .carAProgress("없음")
                .carBProgress("없음")
                .faultA(0)
                .faultB(0)
                .title("기타 사고")
                .laws("해당 없음")
                .precedents("없음")
                .build());
    }
}
