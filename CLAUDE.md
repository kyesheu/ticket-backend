# CLAUDE.md

## 项目

基于 RuoYi-Vue Spring Boot 3 二开的企业工单管理系统。v1.0 新增 `ruoyi-ticket` 模块，实现工单创建、分派、处理、确认、关闭/取消、评论、操作日志主流程。

必要时可对基础模块做小范围、可回滚的调整。

## 文档

改代码前按任务读对应文档，按 `docs/04-implementation-plan.md` 分阶段推进，不跳阶段。

| 文档 | 内容 |
|---|---|
| `docs/01-project-spec.md` | 项目边界、验收标准 |
| `docs/02-architecture-design.md` | 分层规范、命名约定 |
| `docs/03-database-design.md` | 表结构、索引 |
| `docs/04-implementation-plan.md` | 8 阶段实施计划 |
| `docs/05-test-release.md` | 测试清单 |

## 代码规范

写 Java 前先加载 `alibaba-java-coding-guidelines-skill`，按规范生成，不事后审查。

- POJO（Domain / DTO / VO）必须实现 `Serializable`，声明 `serialVersionUID`
- 类名 UpperCamelCase，方法/变量 lowerCamelCase，常量 UPPER_SNAKE_CASE
- 不用魔法值，固定值用枚举
- 注释用中文 Javadoc
- Service 接口 `I` 前缀，实现 `Impl` 后缀

## 开发和测试流程

每次开发前先说明：哪个阶段、改哪些文件、为什么、是否涉及基础模块、如何验证。一次一个阶段。

### 1. 列测试用例

合法场景 + 非法场景 + 边界条件。先想清楚要测什么。

### 2. 建代码骨架

**只建当前功能需要的类**，不一次性生成全部。保证编译通过即可。

- 非空壳：临时实现必须写 `// TODO: 阶段X实现`，不能留空方法体导致编译错误
- 骨架不算完成——只是让后续步骤能编译的前提

### 3. 写单元测试

核心规则必测：状态流转、权限判断、参数校验。Service 层用 Mockito mock Mapper（Mapper = 数据库边界，mock 合理）。测行为不测实现。

### 4. 实现业务逻辑

填充 Service Impl 和 Controller 方法体，使测试通过。

### 5. `mvn test` → `mvn clean compile`

全部通过才能继续。

### 6. 启动后端 + 接口 smoke

启动 Spring Boot，执行 `scripts/ticket/smoke-test.ps1`，覆盖：分类 CRUD、工单主流程、合法/非法流转、评论、日志、权限。

**没有测试结果，不声称功能完成。**
