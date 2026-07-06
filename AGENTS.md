# 项目规则

基于 RuoYi-Vue Spring Boot 3 的企业工单系统；业务代码集中在 ruoyi-ticket，AI 能力集中在 ai-service。

## 沟通

- 默认中文；代码、命令、变量和路径保持英文。
- 结论先行，简洁直接；发现问题或更优方案时明确说明。

## 文档与阶段

- 从 docs/README.md 定位版本，读取该版本 01–05 文档。
- 严格按 04-implementation-plan.md 推进，一次只做一个阶段，不跳阶段。
- 开发前说明版本与阶段、文件范围、原因、是否影响基础模块和验证方式。
- 文档、实现和 05-test-release.md 实测记录必须一致。

## Java 规范

- 写 Java、Spring、MyBatis 或 SQL 前加载 alibaba-java-coding-guidelines-skill。
- Domain、DTO、VO 实现 Serializable 并声明 serialVersionUID。
- 类名 UpperCamelCase；方法和变量 lowerCamelCase；常量 UPPER_SNAKE_CASE。
- 固定值使用枚举，不使用魔法值；注释使用中文 Javadoc。
- Service 接口使用 I 前缀，实现使用 Impl 后缀。

## 开发顺序

1. 列出合法、非法和边界测试用例。
2. 只建立当前功能需要的骨架；临时实现标记 TODO: 阶段X实现，禁止空方法。
3. 先写核心规则测试：状态、权限、参数和事务边界；Service 测试可 mock Mapper。
4. 实现业务逻辑，使目标测试通过。
5. 执行 mvn test 和 mvn clean compile。
6. 按版本 05-test-release.md 启动依赖并执行 smoke。

没有实际测试结果，不得声称阶段或版本完成。

## Git 与红线

- 不自动 commit 或 push；提交前先展示变更摘要，commit message 使用简洁英文。
- 删除文件/目录或 Git 历史、修改 .env/密钥/token/证书/CI/CD、push、rebase、reset --hard、强制推送、公开发布前必须先确认。
