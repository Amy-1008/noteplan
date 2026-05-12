# NotePlan 记事本日程管理

轻量笔记与日程一体化管理工具（课程设计）。前后端分离：Vue3 + Vite 前端，Spring Boot + MyBatis 后端，MySQL 8.0 持久化。

## 核心功能（SRS）

- 备忘录管理（含版本保留至多 3 条、软删除与日程关联解除）
- 备忘录版本管理（查看、恢复、删除历史版本）
- 日程管理（提醒时间精确到分钟、完成状态、关联备忘录）
- 日历视图（月视图、按日聚合笔记与日程）
- 标签管理（去重、关联、筛选）
- 关键词搜索（模糊匹配、结果高亮由前端实现）

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Vue Router、Axios |
| 后端 | JDK 11、Spring Boot 2.7、MyBatis、MySQL 8.0 |
| 协议 | HTTP/1.1、RESTful、JSON、UTF-8 |

## 环境要求

- JDK 11、Maven 3.8+
- Node.js、npm
- MySQL 8.0（数据库名 `noteplan`，端口 3306）

## 启动顺序

1. 启动 MySQL，执行 `sql/schema.sql` 初始化库表。
2. 后端：`cd backend && mvn spring-boot:run` → [http://localhost:8080](http://localhost:8080)
3. 前端：`cd frontend && npm install && npm run dev` → [http://localhost:5174](http://localhost:5174)

端口冲突时：后端修改 `backend/src/main/resources/application.yml` 中 `server.port`；前端修改 `frontend/vite.config.js` 中 `server.port`。

## 仓库结构

```
noteplan/
├── README.md
├── sql/schema.sql          # 数据库建表脚本
├── backend/                # Spring Boot 工程
└── frontend/               # Vue3 + Vite 工程
```

## 项目组

叶乐萱、冯玉瑶、陈薇、曹韶涵、吴思雨、韩茜蕾、王晨曦、钟君思佳。

## 文档依据

《NotePlan 记事本日程管理 APP 项目章程》（NP-PRO-20260420）及配套 SRS、开发计划等内部文档。
