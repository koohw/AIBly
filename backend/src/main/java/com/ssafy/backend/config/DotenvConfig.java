package com.ssafy.backend.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;

public class DotenvConfig {

    public static void loadEnv() {
        try {
            File currentDir = new File(".").getAbsoluteFile();

            // 최대 3단계 상위까지 탐색
            for (int i = 0; i <= 3; i++) {
                File targetDir = currentDir;
                for (int j = 0; j < i; j++) {
                    targetDir = targetDir.getParentFile();
                }

                // .env와 env 두 파일 모두 검사
                for (String fileName : new String[]{".env", "env"}) {
                    File envFile = new File(targetDir, fileName);
                    if (envFile.exists()) {
                        Dotenv dotenv = Dotenv.configure()
                                .directory(targetDir.getAbsolutePath())
                                .filename(fileName)
                                .load();

                        dotenv.entries().forEach(entry -> {
                            if (System.getenv(entry.getKey()) == null) {
                                System.setProperty(entry.getKey(), entry.getValue());
                            }
                        });

                        System.out.println("로컬 환경: 환경 변수 파일을 로드했습니다: " + envFile.getAbsolutePath());
                        return; // 첫 번째 발견된 파일만 로드하고 종료
                    }
                }
            }

            System.out.println("CI/CD 환경: .env 또는 env 파일이 없어 Jenkins Credentials 사용");

        } catch (Exception e) {
            System.out.println("환경 변수 로드 중 오류 발생: " + e.getMessage());
        }
    }
}
