---
name: ticket-git-flow
description: >
  当前项目 ticket-backend 的 Git 开发流程封装。用于开 feature 分支、检查改动、提交、合并、推送。
  当用户提到开分支、提交、commit、合并、merge、push、上传 GitHub、提交当前功能、完成当前阶段时使用此 skill。
  也适用于任何涉及 git 操作的开发工作流场景。
---

# ticket-git-flow

ticket-backend 项目的 Git 开发流程。确保每次改动有清晰的分支、干净的提交历史和编译验证。

## 开发前

每次开始写代码前，按顺序执行：

1. `git status` — 确认工作区状态。如果有未提交的改动，先向用户说明当前改动内容，不要自动 commit、stash、reset
2. 阅读 `docs/04-implementation-plan.md` — 确认当前处于哪个阶段
3. 向用户说明：
   - 当前阶段
   - 准备创建或切换的分支名
   - 预计修改哪些文件
   - 如何验证

**分支规则**：

- 每个功能从 `main` 新建 feature 分支，命名格式：`feature/<阶段名>` 或 `feature/<功能简述>`
- 例如：`feature/ticket-category`、`feature/ticket-workflow`、`feature/comment-log`
- **禁止直接在 `main` 上开发**

**危险命令**：禁止自动执行以下命令，除非用户明确要求并确认：

- `git reset --hard`
- `git clean -fd`
- `git rebase`
- `git push --force`
- `git checkout .`
- 删除分支（`git branch -D` / `git branch -d`）

## 开发完成后

功能完成后，按顺序执行：

1. `mvn clean compile` — **必须通过**，否则不能提交
2. `git status` — 确认改动文件符合预期
3. `git diff` — 确认未暂存改动内容无误
4. `git diff --staged` — 确认已暂存改动内容无误，检查没有遗留调试代码、硬编码敏感信息

如果 `mvn clean compile` 失败：
- 不要提交，先说明失败原因
- 修复后再重新编译
- 通过后才能继续

如果有未预期的文件改动，先停下来让用户确认。

## 提交

commit message 使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

| 类型 | 使用场景 |
|---|---|
| `feat:` | 新增功能 |
| `fix:` | 修复 bug |
| `docs:` | 文档变更 |
| `chore:` | 构建、依赖、模块结构调整 |
| `test:` | 测试代码 |
| `refactor:` | 重构（不改变功能） |
| `style:` | 代码格式（不影响逻辑） |

**示例**：

```
docs: add ticket design docs
chore: add ruoyi-ticket module skeleton
feat: add ticket category CRUD
feat: add ticket main workflow with status transitions
test: add ticket status transition tests
fix: correct PROCESSING to CANCELLED transition guard
refactor: extract ticketNo generation to separate method
```

提交前先展示将要提交的变更摘要，等用户确认。

## 推送和合并

- **不自动 `git push`**，除非用户明确说"push"或"上传 GitHub"或"推送到远程"
- **不自动合并到 `main`**，除非用户明确说"合并到 main"或"merge to main"
- 如果用户说"上传到 GitHub"或类似表述，先确认分支和提交内容，再执行 `git push origin <当前分支>`
- 合并到 `main` 后必须再次执行 `mvn clean compile`，通过后再 `git push origin main`

## 日常同步

如果需要在开发前同步 `main` 的最新代码：

```bash
git checkout main
git pull origin main
git checkout <feature分支>
git merge main
```

合并前确认工作区干净，合并后有冲突则先解决冲突再继续开发。
