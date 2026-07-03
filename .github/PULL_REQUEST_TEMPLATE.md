## 概述

实现 v2.2 工单附件能力。支持临时上传、工单/评论绑定、列表、受控下载和逻辑删除，
并在事务提交后清理本地物理文件，保留失败补偿状态。

## 变更范围

| 类别 | 内容 |
|---|---|
| 模块 | `ruoyi-ticket`：附件上传、绑定、查询、下载、删除和物理清理补偿 |
| 数据库 | 新增 `ticket_attachment` 表及 4 条附件权限菜单 |
| 配置 | 不修改运行配置；本地默认 `uploadPath/` 加入 `.gitignore` |
| 测试 | 新增附件模型、Service、存储和清理测试；扩展 v2.2 smoke |
| 文档 | 更新项目规格、实施计划和测试发布记录 |

## 测试结果

- **mvn test**：162/162 通过
- **mvn clean compile**：通过
- **mvn package -DskipTests**：通过
- **接口冒烟**：69/69 通过；附件独立 smoke 通过
- **OpenAPI**：包含 4 条附件接口路径

## 不涉及

- 不修改 `ruoyi-common`、`ruoyi-framework`、`ruoyi-system` 等基础模块。
- 不修改 `.env`、密钥、CI/CD 或运行配置。
- 不改变工单状态机、SLA、通知、满意度、动态流程和自定义字段规则。
- 不包含分片上传、断点续传、在线预览、病毒扫描、对象存储或 ES。

## Checklist

- [ ] CI 通过
- [x] Code Review 通过
- [x] `sql/ticket-v2.2.sql` 已在本地 Docker MySQL 执行
- [x] 单元测试、编译、打包和完整 smoke 通过

---

🤖 Generated with Codex
