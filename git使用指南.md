# Git 使用指南

本指南旨在提供一个清晰、全面的 Git 操作参考，覆盖从基础配置到日常使用的核心命令。

---

### 一、Git 核心概念

理解 Git 的工作流程至关重要。主要涉及四个关键区域：

1.  **工作区 (Workspace)**: 您在电脑上能看到的项目目录，是您当前进行代码修改的地方。
2.  **暂存区 (Staging Area / Index)**: 一个临时的存储区域，用于存放您希望在下一次提交中包含的更改。`git add` 命令就是将工作区的修改添加到暂存区。
3.  **本地仓库 (Local Repository)**: 存储在您本地计算机上的项目版本历史。`git commit` 命令将暂存区的更改永久保存到本地仓库。
4.  **远程仓库 (Remote Repository)**: 托管在网络服务器上的项目版本库（如 GitHub, Gitee），用于团队协作和数据备份。

---

### 二、基础配置

在首次使用 Git 前，需要配置您的用户信息，这些信息会记录在每次提交中。

```bash
# 配置用户名
git config --global user.name "Your Name"

# 配置用户邮箱
git config --global user.email "your.email@example.com"

# 查看所有配置信息
git config --list
```

---

### 三、常用命令详解

#### 1. 仓库初始化与克隆

*   **`git init`**: 在当前目录创建一个新的 Git 仓库。
    ```bash
    # 进入项目目录
    cd my-project
    # 初始化仓库
    git init
    ```

*   **`git clone`**: 从远程仓库复制一份完整的项目到本地。
    ```bash
    # 克隆远程仓库
    git clone https://github.com/username/repository.git
    ```

#### 2. 文件状态与提交 (日常核心)

*   **`git status`**: 查看工作区和暂存区的文件状态（未跟踪、已修改、已暂存等）。
    ```bash
    git status
    ```

*   **`git add`**: 将文件的修改从工作区添加到暂存区。
    ```bash
    # 添加指定文件
    git add <file_name>

    # 添加当前目录下的所有更改
    git add .
    ```

*   **`git commit`**: 将暂存区的更改提交到本地仓库，并附带一条描述信息。
    ```bash
    # 提交并打开默认编辑器输入描述
    git commit

    # 提交并直接附带描述信息
    git commit -m "Your commit message"
    ```

*   **`git log`**: 查看提交历史记录。
    ```bash
    # 查看详细历史
    git log

    # 以单行简洁模式查看
    git log --oneline
    ```

#### 3. 分支管理

分支是 Git 的强大功能，允许多人并行开发而互不干扰。

*   **`git branch`**: 列出、创建或删除分支。
    ```bash
    # 列出所有本地分支
    git branch

    # 创建一个新分支
    git branch <branch_name>

    # 删除一个分支
    git branch -d <branch_name>
    ```

*   **`git checkout`** 或 **`git switch`** (推荐): 切换分支。
    ```bash
    # 切换到已存在的分支
    git checkout <branch_name>
    # 或者 (推荐使用新命令)
    git switch <branch_name>

    # 创建并立即切换到新分支
    git checkout -b <new_branch_name>
    # 或者 (推荐使用新命令)
    git switch -c <new_branch_name>
    ```

*   **`git merge`**: 将一个分支的更改合并到当前分支。
    ```bash
    # 1. 首先切换到接收更改的分支 (如 main)
    git switch main

    # 2. 合并 feature 分支的更改
    git merge feature-branch
    ```

#### 4. 远程协作

*   **`git remote`**: 管理远程仓库的连接。
    ```bash
    # 查看已配置的远程仓库
    git remote -v

    # 添加一个新的远程仓库
    git remote add <remote_name> <remote_url>
    # 例如: git remote add origin https://github.com/user/repo.git
    ```

*   **`git fetch`**: 从远程仓库下载最新的提交历史和分支信息，但**不**会自动合并到您当前的工作。
    ```bash
    git fetch <remote_name>
    # 例如: git fetch origin
    ```

*   **`git pull`**: 从远程仓库拉取最新更改并自动合并到当前分支。它相当于 `git fetch` + `git merge`。
    ```bash
    git pull <remote_name> <branch_name>
    # 例如: git pull origin main
    ```

*   **`git push`**: 将本地仓库的提交推送到远程仓库。
    ```bash
    git push <remote_name> <branch_name>
    # 例如: git push origin main

    # 如果是首次推送新创建的分支，需要设置上游跟踪
    git push -u origin <new_branch_name>
    ```

---

### 四、撤销与回滚

*   **`git reset`**: 将当前分支的 HEAD 指针移动到指定的提交，同时可以选择性地修改暂存区和工作区。
    ```bash
    # 仅移动 HEAD 指针，保留暂存区和工作区的修改
    git reset --soft <commit_id>

    # 移动 HEAD 并重置暂存区，保留工作区的修改 (常用)
    git reset --mixed <commit_id>

    # 彻底回滚到指定提交，暂存区和工作区都将被覆盖 (危险操作)
    git reset --hard <commit_id>
    ```

*   **`git revert`**: 创建一个新的提交来撤销某个历史提交的更改。这是一种安全的回滚方式，因为它不会改变项目历史。
    ```bash
    git revert <commit_id>
    ```

*   **撤销工作区的修改**:
    ```bash
    # 丢弃某个文件的所有本地修改
    git checkout -- <file_name>
    ```

---

### 五、高级技巧

*   **`git stash`**: 临时保存当前工作区的修改，让工作区恢复到干净状态，以便切换分支或处理紧急任务。
    ```bash
    # 保存当前修改
    git stash

    # 查看储藏列表
    git stash list

    # 恢复最近一次的储藏并从列表中删除
    git stash pop

    # 恢复储藏但不删除
    git stash apply
    ```

*   **`.gitignore` 文件**: 在项目根目录创建一个 `.gitignore` 文件，列出不需要被 Git 跟踪的文件或目录（如 `node_modules`, `target`, `.idea` 等）。

*   **`git diff`**: 查看不同区域之间的差异。
    ```bash
    # 查看工作区与暂存区的差异
    git diff

    # 查看暂存区与最新提交的差异
    git diff --staged

    # 查看两个分支之间的差异
    git diff branch1..branch2
    ```

