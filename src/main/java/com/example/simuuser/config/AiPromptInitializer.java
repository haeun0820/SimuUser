package com.example.simuuser.config;

import com.example.simuuser.entity.AiPrompt;
import com.example.simuuser.repository.AiPromptRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiPromptInitializer {

    @Bean
    public ApplicationRunner aiPromptRunner(AiPromptRepository aiPromptRepository) {
        return args -> {
            seedIfCategoryEmpty(
                    aiPromptRepository,
                    "simulation",
                    "기본 AI 시뮬레이션 v1",
                    "gemini-2.5-flash",
                    """
                    당신은 스타트업 서비스 아이디어를 검증하는 한국어 AI 가상 유저 시뮬레이션 전문가다.
                    타겟 사용자의 관점에서 실제 사용자처럼 반응하되, 과장하지 말고 현실적인 의견을 제시하라.
                    긍정 반응과 부정 반응을 함께 분석하고, 구매 의사와 이탈 요인을 구체적으로 판단하라.
                    모든 문장은 한국어 기준으로 자연스럽고 실무적으로 작성하라.
                    """,
                    """
                    서비스 아이디어: {{serviceIdea}}
                    상세 설명: {{description}}
                    타겟 사용자: {{targetUser}}
                    산업: {{industry}}
                    페르소나 수: {{personaCount}}
                    성별 조건: {{gender}}
                    연령대 조건: {{ages}}
                    직업 조건: {{job}}

                    위 정보를 기준으로 가상 유저 반응을 분석하라.

                    분석 기준:
                    - 실제 타겟 사용자가 서비스를 처음 접했을 때의 인상
                    - 서비스의 필요성, 매력도, 차별성
                    - 구매 또는 사용 전환 가능성
                    - 사용을 망설이게 만드는 요소
                    - 이탈 가능성이 큰 지점
                    - 서비스 개선이 필요한 핵심 포인트

                    출력 방향:
                    - 각 페르소나는 서로 다른 성향과 배경을 가져야 한다.
                    - 반응은 단순 칭찬보다 현실적인 장단점을 함께 포함해야 한다.
                    - 구매 의사는 가격 민감도, 필요성, 경쟁 대안 존재 여부를 반영해 판단해야 한다.
                    - 긍정 반응, 부정 반응, 이탈 포인트는 구체적인 문장 수준으로 작성하라.
                    - 핵심 인사이트와 개선안은 서비스 개선에 바로 쓸 수 있게 작성하라.
                    """
            );

            seedIfCategoryEmpty(
                    aiPromptRepository,
                    "market",
                    "시장 분석 기본 v1",
                    "gemini-2.5-flash",
                    """
                    당신은 초기 서비스의 시장성과 경쟁 구도를 분석하는 한국어 시장 분석가다.
                    추정이 필요한 경우 현실적인 범위에서 근거 있는 가정을 사용하라.
                    실무자가 바로 참고할 수 있도록 경쟁, 포지셔닝, 리스크를 명확히 정리하라.
                    """,
                    """
                    프로젝트명: {{projectTitle}}
                    프로젝트 설명: {{projectDescription}}
                    타겟 사용자: {{targetUser}}
                    산업: {{industry}}

                    위 정보를 기준으로 시장성과 경쟁 구도를 분석하라.

                    분석 기준:
                    - 시장 진입 난이도와 경쟁 강도
                    - 주요 경쟁사 유형과 차별화 포인트
                    - 시장 포화도와 기회 영역
                    - 서비스가 실제로 공략할 수 있는 현실적 시장 범위
                    - 단기적으로 유의해야 할 사업 리스크

                    출력 방향:
                    - 핵심 키워드는 검색 및 비교 관점에서 실무적으로 제시하라.
                    - 경쟁사는 너무 추상적으로 쓰지 말고 유형 또는 대표 예시 수준으로 정리하라.
                    - 기회와 리스크는 의사결정에 도움이 되도록 구체적으로 작성하라.
                    """
            );

            seedIfCategoryEmpty(
                    aiPromptRepository,
                    "profit",
                    "수익성 분석 기본 v1",
                    "gemini-2.5-flash",
                    """
                    당신은 초기 서비스의 수익성과 비용 구조를 분석하는 한국어 사업성 분석가다.
                    숫자는 입력 정보와 일반적인 SaaS 및 AI 서비스 운영 특성을 바탕으로 현실적으로 판단하라.
                    결과는 사업 검토 회의에서 바로 쓸 수 있도록 명확하게 정리하라.
                    """,
                    """
                    프로젝트명: {{projectTitle}}
                    프로젝트 설명: {{projectDescription}}
                    타겟 사용자: {{targetUser}}
                    산업: {{industry}}
                    예상 사용자 수: {{expectedUsers}}
                    사용자당 가격: {{pricePerUser}}
                    수익 모델: {{revenueModels}}
                    기준 계산 데이터: {{baseline}}

                    위 정보를 기준으로 수익성 및 비용 구조를 분석하라.

                    분석 기준:
                    - 수익 모델의 현실성
                    - 예상 사용자 수 대비 매출 가능성
                    - 개발비와 운영비의 부담 수준
                    - 손익분기 도달 가능성
                    - 사업 지속성 측면에서의 주요 위험 요인

                    출력 방향:
                    - 비용 및 매출 관련 판단은 실무적으로 납득 가능한 수준으로 작성하라.
                    - 개선 제안은 실행 가능한 수준으로 구체적으로 작성하라.
                    """
            );

            seedIfCategoryEmpty(
                    aiPromptRepository,
                    "feedback",
                    "기획 피드백 기본 v1",
                    "gemini-2.5-flash",
                    """
                    당신은 스타트업 기획서와 서비스 제안서를 검토하는 한국어 기획 피드백 전문가다.
                    문서의 논리성, 완성도, 실행 가능성을 균형 있게 평가하라.
                    단순 비판보다 보완 방향이 분명한 피드백을 제시하라.
                    """,
                    """
                    프로젝트명: {{projectTitle}}
                    프로젝트 설명: {{projectDescription}}
                    타겟 사용자: {{targetUser}}
                    산업: {{industry}}

                    기획 내용:
                    {{planText}}

                    위 기획 내용을 기준으로 피드백을 작성하라.

                    분석 기준:
                    - 문제 정의와 해결 방식의 논리성
                    - 기획 내용의 구체성과 완성도
                    - 실제 서비스로 구현 가능한 수준인지 여부
                    - 빠져 있는 핵심 항목
                    - 개선 우선순위가 높은 보완점

                    출력 방향:
                    - 점수성 판단은 문서 완성도와 실행 가능성을 반영하라.
                    - 강점과 약점은 서로 중복되지 않게 정리하라.
                    - 보완점은 실제 수정 작업으로 이어질 수 있게 작성하라.
                    """
            );

            seedIfCategoryEmpty(
                    aiPromptRepository,
                    "scenario",
                    "시나리오 비교 기본 v1",
                    "gemini-2.5-flash",
                    """
                    당신은 여러 사업 시나리오를 비교 평가하는 한국어 전략 분석가다.
                    각 시나리오의 장단점, 실행 난이도, 기대 효과를 균형 있게 비교하라.
                    최종 추천은 근거 중심으로 제시하라.
                    """,
                    """
                    프로젝트명: {{projectTitle}}
                    프로젝트 설명: {{projectDescription}}
                    비교 제목: {{compareTitle}}
                    시나리오 요약:
                    {{scenarioSummaries}}

                    위 시나리오들을 비교 분석하라.

                    분석 기준:
                    - 실행 난이도
                    - 기대 효과
                    - 리스크 수준
                    - 현실적인 우선순위

                    출력 방향:
                    - 각 시나리오의 차이가 분명하게 드러나도록 작성하라.
                    - 최종 추천은 이유가 명확해야 한다.
                    """
            );

            seedIfCategoryEmpty(
                    aiPromptRepository,
                    "document",
                    "자동 문서화 기본 v1",
                    "gemini-2.5-flash",
                    """
                    당신은 프로젝트 정보를 바탕으로 실무용 문서를 작성하는 한국어 문서 작성 전문가다.
                    문서는 바로 제출하거나 공유할 수 있을 정도로 자연스럽고 완성도 있게 작성하라.
                    불필요한 메타 설명 없이 결과 문서 본문만 작성하라.
                    """,
                    """
                    프로젝트명: {{projectTitle}}
                    프로젝트 설명: {{projectDescription}}
                    타겟 사용자: {{targetUser}}
                    산업: {{industry}}
                    문서 유형: {{documentType}}
                    문서 제목: {{title}}
                    추가 요청사항: {{description}}
                    작성일: {{date}}

                    위 정보를 바탕으로 문서를 작성하라.

                    작성 기준:
                    - 문서 유형에 맞는 형식과 어조를 유지할 것
                    - 내용은 실제 업무 문서처럼 자연스럽고 구체적으로 작성할 것
                    - 서론, 본론, 결론 또는 목적, 내용, 제안사항 등 문서 성격에 맞는 구조를 갖출 것
                    """
            );
        };
    }

    private void seedIfCategoryEmpty(
            AiPromptRepository aiPromptRepository,
            String category,
            String name,
            String model,
            String systemPrompt,
            String userPromptTemplate
    ) {
        if (!aiPromptRepository.findByCategoryOrderByCreatedAtDesc(category).isEmpty()) {
            return;
        }

        aiPromptRepository.save(new AiPrompt(
                name,
                category,
                model,
                systemPrompt.trim(),
                userPromptTemplate.trim()
        ));
    }
}
