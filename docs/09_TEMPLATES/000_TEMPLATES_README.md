# ?�� 09_TEMPLATES 문서 ?�덱??

> **?�더�?*: 09_TEMPLATES (?�로?�트 ?�플�?& 가?�드)  
> **목적**: ???�로?�트 ?�작 ???�요??모든 ?�플�??�공  
> **관리자**: Team Lead  
> **최종 ?�데?�트**: 2026-04-08  

---

## ?�� ?�더 개요

???�더??**???�로?�트 ?�는 ??기능 개발**???�요??모든 ?�플릿을 ?�함?�니??

### 목적
- ??개발 ?�명주기 가?�드 (Phase 1-4)
- ??체크리스??(?�계/구현/검�?
- ???�보???�료 (?�규 ?�??교육)
- ??문제 ?�결 가?�드 (Q&A)
- ???�정 가?�드 (Ollama, Docker ??

---

## ?�� 문서 목록

### **메인 가?�드**

| # | 문서�?| Link | 개요 |
| --- | --- | --- | --- |
| **000** | **TEMPLATES ?�덱??* | [000_TEMPLATES_README.md](./000_TEMPLATES_README.md) | ???�더??개요 |
| **010** | **Quick Reference** | [010_QUICK_REFERENCE_TEMPLATE.md](./010_QUICK_REFERENCE_TEMPLATE.md) | ?�눈??보는 개발 ?�름 |
| **020** | **Onboarding Guide** | [020_ONBOARDING_TEMPLATE.md](./020_ONBOARDING_TEMPLATE.md) | �?기능 개발 가?�드 (4-5?�간) |
| **030** | **Troubleshooting** | [030_TROUBLESHOOTING_TEMPLATE.md](./030_TROUBLESHOOTING_TEMPLATE.md) | 문제 ?�결 Q&A |

### **Phase�?체크리스??* (050_CHECKLISTS_TEMPLATE/)

| # | 문서�?| ?�도 |
| --- | --- | --- |
| **051** | **Phase 1 Design** | ?�계 ?�계 (Self Check) |
| **052** | **Phase 2 Execute** | 구현 ?�계 (Self Test) |
| **053** | **Phase 3 Verify** | 검�??�계 (Claude 검�? |
| **054** | **Commit** | 커밋 ??최종 ?�인 |

### **?�정 가?�드** (040_SETUP_TEMPLATE/)

| # | 문서�?| ?�도 |
| --- | --- | --- |
| **041** | **Ollama 직접 ?�치** | 로컬 개발 ?�경 (권장) |
| **042** | **Docker Compose** | ?� ?��???배포 ?�경 |

---

## ?�� ?�더 구조

```
09_TEMPLATES/
?��??� 000_TEMPLATES_README.md (???�일)
?��??� 010_QUICK_REFERENCE_TEMPLATE.md
?��??� 020_ONBOARDING_TEMPLATE.md
?��??� 030_TROUBLESHOOTING_TEMPLATE.md
?��??� 040_SETUP_TEMPLATE/
??  ?��??� 041_OLLAMA_DIRECT_INSTALL_TEMPLATE.md
??  ?��??� 042_DOCKER_COMPOSE_OLLAMA_TEMPLATE.md
?��??� 050_CHECKLISTS_TEMPLATE/
    ?��??� 051_PHASE_1_DESIGN_CHECKLIST.md
    ?��??� 052_PHASE_2_EXECUTE_CHECKLIST.md
    ?��??� 053_PHASE_3_VERIFY_CHECKLIST.md
    ?��??� 054_COMMIT_CHECKLIST.md
```

---

## ?? ?�용 ?�나리오

### **?�나리오 1: ???�로?�트 ?�작**

```
Step 1: 010_QUICK_REFERENCE ?�기 (5�?
  ?��? 개발 ?�름 ?�해

Step 2: 040_SETUP_TEMPLATE ?�행 (15-20�?
  ?��? 041_OLLAMA_DIRECT_INSTALL (로컬)
  ?��? 042_DOCKER_COMPOSE (?� ?�업, ?�택)

Step 3: 020_ONBOARDING ?�라?�기 (4-5?�간)
  ?��? Phase 1: ?�계 + Self Check
  ?��? Phase 2: 구현 + Self Test
  ?��? Phase 3: Claude 검�?
  ?��? Phase 4: 커밋

Result: �?기능 ?�성! ??
```

### **?�나리오 2: 문제 발생**

```
문제 발생
  ??
030_TROUBLESHOOTING_TEMPLATE?�서 검??
  ?��? 찾음 ???�결 방법 ?�행
  ?��? �?찾음 ???� 리더 ?�담
```

### **?�나리오 3: �?Phase 진행**

```
Phase 1 ?�계: 051_PHASE_1_DESIGN_CHECKLIST ?�용
Phase 2 구현: 052_PHASE_2_EXECUTE_CHECKLIST ?�용
Phase 3 검�? 053_PHASE_3_VERIFY_CHECKLIST ?�용
커밋 ?? 054_COMMIT_CHECKLIST ?�인
```

---

## ?�� ?�플�?커스?�마?�징

### PM/Leader가 ?�야 ????

???�로?�트마다:

```
1. ???�더??모든 ?�플�?복사
2. ?�음 ??�� 변�?
   - [PROJECT_NAME] ???�제 ?�로?�트�?
   - [TECH_STACK] ??기술?�택
   - [TEAM_SIZE] ???� 규모
   - [TODO] ???�로?�트�?추�? ??��
3. ?�??배포
4. ?�보???�작 (020_ONBOARDING ?�용)
```

---

## ?�� 권장 ?�용 ?�간

| ??�� | ?�요 ?�간 |
| --- | --- |
| Quick Reference ?�기 | 5�?|
| ?�정 (040_SETUP) | 15-20�?|
| Onboarding (020_) | 4-5?�간 |
| 문제 ?�결 (030_) | ?�황�?|
| 체크리스???�용 | Phase�?30�?2?�간 |

---

## ?�� ?�합 ?�름

```
???�로?�트/기능 ?�작
       ??
010_QUICK_REFERENCE (5�? - ?�름 ?�해
       ??
040_SETUP_TEMPLATE (20�? - ?�경 ?�정
       ??
020_ONBOARDING (4-5?�간) - Phase 1-4 진행
  ?��? 051 체크리스??(Phase 1)
  ?��? 052 체크리스??(Phase 2)
  ?��? 053 체크리스??(Phase 3)
  ?��? 054 체크리스??(커밋)
       ??
?�료! ??
```

---

## ?�� 관??링크

- [00_GUIDE/000_README.md](../00_GUIDE/000_README.md) - ?�체 개발 방법�?
- [00_GUIDE/201_SAR_RULE.md](../00_GUIDE/201_SAR_RULE.md) - SAR ?�성 규칙
- [08_Self_Audit/001_Self_Audit_Overview.md](../08_Self_Audit/001_Self_Audit_Overview.md) - ?�류 관�?

---

**버전**: v1.0 | **최종 ?�데?�트**: 2026-04-08

