---
name: architecture-review
description: DDD 아키텍처 구조 검토
---

현재 프로젝트 구조를 기준으로 검토한다.

패키지 구조:
- authorization
- product
- order
- webhook

계층:
- presentation
- application
- domain
- infrastructure

검토 항목:
- domain → infrastructure 의존 여부
- domain → presentation 의존 여부
- application 책임 과다 여부
- aggregate 경계 적절성
- entity 위치 적절성
- repository 위치 적절성
- dto 위치 적절성

위반 사항 발견 시 수정안을 제시한다.