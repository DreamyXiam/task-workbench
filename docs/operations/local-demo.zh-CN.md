# Windows 本地演示

本地演示使用独立的 H2 数据库和合成数据。先在 `frontend` 目录执行 `npm ci` 安装依赖，再返回仓库根目录。将示例中的可执行文件路径替换为本机已安装的、已修补的 Java 21 和 Node 24 路径：

```powershell
./scripts/start-demo.ps1 -Demo -JavaPath 'C:\your-jdk-21\bin\java.exe' -NodePath 'C:\your-node-24\node.exe' -MavenPath 'C:\your-maven\bin\mvn.cmd'
```

已安装依赖的本地 H2 演示也接受 Node 22 系列中的 22.23.4 及以上版本，或 Node 24 系列中的 24.22.1 及以上版本。依赖安装和生产环境基线仍为 Node 24；Java 21.0.2/Node 22.23.4 的验证仅适用于本地演示。

启动脚本会构建后端，安全提示输入独立的初始密码；如无演示专用 TOTP 密钥，会生成密钥并用 Windows DPAPI 保护本地副本，然后在后台启动前后端。脚本会检查端口、仅绑定本机回环地址，并将数据库和附件固定在 `.task-storage/demo-<后端端口>/` 下，继承的数据库配置无法将演示指向 MySQL。脚本会输出访问地址、进程 ID、日志位置和停止命令，不打印凭据，也不会安装运行时、服务或依赖。

已构建并验证 JAR 后，可加 `-UseBuiltArtifacts` 复用它。如果默认端口 8080/5173 已被占用，可指定 `-BackendPort 18080 -FrontendPort 15173`；Vite 会连接所选后端端口。

脚本会移除继承的 Spring 配置覆盖项，并在结束时恢复调用进程原有的环境状态。只有账号初始化完成、健康检查就绪后才会报告启动成功。启动配置和进程身份回归脚本位于 `scripts/tests/`；在 Windows PowerShell 5.1 和 PowerShell 7 中停止进程时，均会核对可执行文件和 UTC 创建时间。

打开启动脚本输出的前端地址。空的演示数据库会使用输入的初始密码创建 `demo.leader`、`demo.head`、`demo.employee`、`demo.finance`、`demo.other`、`demo.group` 和 `demo.admin`。所有账号首次登录时必须修改密码；领导及管理员账号随后需要绑定 TOTP。项目不提供固定密码。

使用启动时输出的 `./scripts/stop-demo.ps1 -StateFile '<输出的状态文件路径>'` 停止演示。脚本会在终止进程树前核对进程身份。同一 Windows 用户在同一台机器上重启时会复用受 DPAPI 保护的演示密钥；迁移演示数据库时需私下妥善保留原密钥，冲突密钥会被拒绝。进程状态文件不会保存密钥或密码。


返回[项目首页](../../README.zh-CN.md)。
