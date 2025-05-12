package com.ssafy.backend.ai.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.ssafy.backend.ai.dto.AccidentDefinitionDto;
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
        try (
                InputStream in = getClass().getResourceAsStream("/static/case_data.csv");
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                CSVReader csvReader = new CSVReader(reader)
        ) {
            List<String[]> lines = csvReader.readAll();
            lines.remove(0);

            for (String[] parts : lines) {
                if (parts.length < 11) {
                    log.warn("열 수 부족한 라인 무시됨: {}", Arrays.toString(parts));
                    continue;
                }

                try {
                    int code = Integer.parseInt(parts[7].trim());
                    AccidentDefinitionDto def = new AccidentDefinitionDto(
                            parts[1], parts[2], parts[3], parts[4],
                            Integer.parseInt(parts[5].trim()),
                            Integer.parseInt(parts[6].trim()),
                            parts[8], parts[9], parts[10]
                    );
                    accidentMap.put(code, def);
                } catch (Exception e) {
                    log.warn("CSV 한 줄 파싱 실패: {}", Arrays.toString(parts), e);
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("CSV 파일 로딩 실패", e);  // 예외 감싸서 명시 처리
        }
    }

    // ✅ 반드시 public
    public AccidentDefinitionDto get(int code) {
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
