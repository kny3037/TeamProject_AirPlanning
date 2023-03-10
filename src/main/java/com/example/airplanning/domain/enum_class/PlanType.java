package com.example.airplanning.domain.enum_class;

public enum PlanType {
    WAITING,    // 수락 대기 플랜
    ACCEPT,     // 플랜 요청 수락
    REFUSE,     // 플랜 요청 거절
    COMPLETE    // 결제 완료, 신청 완료 플랜
    ;
}