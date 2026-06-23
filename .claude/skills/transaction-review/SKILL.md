---
name: transaction-review
description: 트랜잭션 검토
---

주문, 상품, 웹훅 관련 코드를 분석한다.

확인:
- @Transactional 위치
- 롤백 정책
- 이벤트 발행 시점
- DB 정합성
- 중복 저장 가능성
- 동시성 문제

실무 관점에서 위험 요소를 설명한다.