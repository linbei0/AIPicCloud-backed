## 项目简介

AiPicCloud 是一个基于 Spring Boot 的云端图片管理服务系统，提供安全可靠的图片存储、高效的图片搜索分析、多用户权限管理以及分布式数据处理能力。
!(https://github.com/linbei0/AIPicCloud-backed/blob/master/picture/Home.png)
!(https://github.com/linbei0/AIPicCloud-backed/blob/master/picture/space.png)
### 核心功能
- 图片上传与管理
- 用户注册与登录
- 基于 Sa-Token 的权限认证
- 图片空间管理与成员管理
- 图片分析与统计
- WebSocket 实时通信
- 数据库分库分表支持

## 后端技术栈

- **框架**: Spring Boot 2.7.6
- **数据库**: MySQL 8.x + ShardingSphere 5.2.0
- **缓存**: Redis 6.x + Caffeine
- **权限**: Sa-Token 1.39.0
- **工具**: MyBatis Plus 3.5.9, Hutool 5.8.26, Lombok
- **其他**: WebSocket, Disruptor, jsoup

## 环境要求
1.   JDK 17
2.   MySQL 8.0+
3.   Redis 6.x+
4.   Maven 3.6+

### 接口文档
访问 [Knife4j API 文档](http://localhost:8080/doc.html) 查看和测试接口

## 贡献指南
欢迎提交 Pull Request！请遵循以下规范：
1. Fork 项目并创建新分支
2. 提交代码前确保单元测试通过
3. 遵循项目代码风格
4. 添加必要的注释和文档
