---
name: webhook-review
description: webhook 설계 검토
---

확인:
- 멱등성(idempotency)
- 중복 이벤트 처리
- 재시도 전략
- 이벤트 저장
- 장애 복구

주문 웹훅 기준으로 분석한다.