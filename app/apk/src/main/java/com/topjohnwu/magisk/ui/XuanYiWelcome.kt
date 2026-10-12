package com.topjohnwu.magisk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RAINBOW = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF0000),
        Color(0xFFFF7F00),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFF8B00FF)
    )
)

@Composable
fun XuanYiWelcomeDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "🌈 免责声明 · 应用介绍 🌈",
                style = TextStyle(
                    brush = RAINBOW,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = WELCOME_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("我已阅读并同意")
            }
        }
    )
}

private val WELCOME_TEXT = """
═══════════════════════════════
    XuanYi · 免责声明与应用介绍
═══════════════════════════════

【一、免责声明】

1. 本应用（以下称"XuanYi"）是基于 Magisk 开源项目（github.com/topjohnwu/Magisk）二次修改构建的版本，仅供学习、研究与技术交流使用。任何个人或组织不得将本应用用于任何商业用途、非法用途或违反当地法律法规的用途。

2. 本应用的编译、构建、修改完全由第三方开发者在 GitHub Actions 云平台上自动完成，所使用的源代码均来自公开渠道。开发者不对代码的实际运行效果、稳定性、安全性做出任何形式的保证。

3. 使用本应用需要对 Android 系统底层原理有一定了解。刷入、安装、使用过程中可能出现设备无法开机、系统崩溃、数据丢失、系统损坏、保修失效等风险，这些风险由用户自行承担。

4. 开发者不对使用本应用造成的任何直接或间接损失负责，包括但不限于：设备硬件损坏、数据丢失、账户被盗、业务中断、经济亏损、法律纠纷等。

5. Root 权限（超级用户）是 Android 系统的最高权限，可以修改系统文件、读写所有应用数据、拦截网络请求、注入恶意代码。使用 Root 权限前请务必明确自己的操作意图，不要授予来源不明的应用 Root 权限。

6. 本应用的"签名校验禁用"是为了让修改版 App 能够与官方 magiskd 守护进程通信。此修改会降低设备的整体安全性，可能被恶意程序利用。请仅在充分了解风险的前提下使用。

7. 本应用包含的一切第三方组件、开源代码、素材资源，版权均归原作者所有。如原作者认为本应用的使用侵犯了其合法权益，请联系开发者，我们将在第一时间处理。

8. 本免责声明的最终解释权归开发者所有。开发者有权在不另行通知的情况下修改本声明的内容。

9. 用户下载、安装、使用本应用的行为，即表示已完整阅读、理解并同意本免责声明的全部条款。如不同意，请立即卸载并停止使用。

10. 本应用与 Magisk 官方团队无任何隶属、合作、授权关系，Magisk 及其相关商标归 topjohnwu 所有。

═══════════════════════════════
【二、应用介绍】
═══════════════════════════════

XuanYi 是一款基于 Magisk 修改的 Android Root 管理应用，主要功能与官方 Magisk 保持一致，同时在外观与体验上进行了定制化调整。

XuanYi 的核心是 MagiskSU —— 一套用户级的 Root 权限管理方案。相比传统 Root 方案（如 SuperSU），MagiskSU 采用"系统无修改"（systemless）的方式，通过修改 boot 镜像将 Root 权限注入系统，不直接修改 /system 分区。

这样做的好处是：

· 可以隐藏 Root 状态，让依赖 SafetyNet / Play Integrity 的应用（如银行 App、游戏）正常工作。

· 支持 OTA 系统更新，升级后重新修补 boot 即可保留 Root。

· 卸载更方便，只需恢复原始 boot 镜像。

═══════════════════════════════
【三、主要功能模块】
═══════════════════════════════

◆ 超级用户管理
查看已获得 Root 权限的应用列表，管理每个应用的授权状态，设置"允许 / 拒绝 / 提示"三种策略，查看详细的授权日志。

◆ 模块管理
支持安装、卸载、启用、禁用 Magisk 模块。模块可以修改系统文件、注入代码、替换资源，实现各种自定义功能。

◆ Zygisk
Magisk 的 Zygote 注入框架，允许模块在系统启动时进入应用的进程空间，实现更彻底的代码注入与 Hook。

◆ DenyList（排除列表）
对指定应用隐藏 Root 状态，绕过 Root 检测。

◆ 在线更新
从 GitHub 自动检查新版本，一键更新。

◆ 环境检测
显示 Root 状态、Zygisk 状态、Ramdisk 支持、App 签名等关键信息。

═══════════════════════════════
【四、使用须知】
═══════════════════════════════

在开始使用之前，请确认以下几点：

1. 你的设备已解锁 Bootloader。这是刷入第三方 boot 镜像的前提条件，不同品牌解锁方法不同，通常需要前往官方开发者网站申请。

2. 你的设备已备份重要数据。Root 操作有风险，强烈建议先通过 Recovery 或云服务备份。

3. 你的设备有原始 boot 镜像的备份。如果出问题，可以刷回原始 boot 恢复。

4. 你已经了解如何进入 Fastboot 模式、Recovery 模式、Download 模式（不同厂商叫法不同）。

5. 你不依赖此设备进行重要工作。Root 之后可能出现某些应用无法运行，包括银行、支付、企业办公类应用。

【推荐流程】

步骤 1：解锁 Bootloader（会清空所有数据）
步骤 2：提取当前系统版本的 boot.img 或 init_boot.img
步骤 3：用 XuanYi 修补 boot 镜像
步骤 4：通过 Fastboot 刷入修补后的镜像
步骤 5：开机后打开 XuanYi 验证 Root 状态
步骤 6：根据需要安装模块、配置 DenyList

═══════════════════════════════
【五、常见问题】
═══════════════════════════════

Q1：Root 后银行 App 打不开怎么办？
A1：把银行 App 加入 DenyList，重启 App 后重试。

Q2：如何完全卸载 Root？
A2：在 XuanYi 里点击"卸载 Magisk"，或刷回原始 boot 镜像。

Q3：为什么 App 显示"环境异常"？
A3：可能有残留的 Root 痕迹，或 SafetyNet 检测未通过。检查 DenyList 配置。

Q4：可以同时安装官方 Magisk 吗？
A4：不可以。XuanYi 与官方 Magisk 使用相同的包名，无法共存。

Q5：为什么应用签名和官方不一样？
A5：本应用使用独立签名，禁用了 magiskd 的签名校验，因此能与官方 magiskd 通信。这也是安全风险所在。

Q6：为什么第一次打开会显示这个免责声明？
A6：因为 Root 涉及系统底层，风险较高，希望用户在充分了解的情况下使用。每次冷启动都会显示。

═══════════════════════════════
【六、安全提示】
═══════════════════════════════

本应用禁用了 magiskd 的签名校验，这意味着：

· 任何使用相同包名（com.topjohnwu.magisk）的应用，无论签名如何，都能请求 Root 权限。

· 恶意应用可能伪装成 Magisk 来窃取 Root 权限，进而控制整个系统。

· 请勿安装来源不明的 APK，尤其是使用相同包名的 APK。

· 定期检查"超级用户"页面，撤销不认识的授权。

· 如非必要，不要长期开启 Root 权限。

═══════════════════════════════
【七、鸣谢】
═══════════════════════════════

· topjohnwu 及其团队开发并维护 Magisk 开源项目
· KernelSU、APatch、Magisk Delta 等项目的开发者提供的灵感与参考
· GitHub 提供的免费 CI/CD 编译环境
· 每一位提交 Issue、Pull Request 的社区贡献者

═══════════════════════════════
【八、版本信息】
═══════════════════════════════

基于 Magisk v31.0 (31000) 构建
构建方式：GitHub Actions 云编译
构建平台：Ubuntu 24.04 + JDK 21

═══════════════════════════════

最后再次提醒：Root 有风险，操作需谨慎。

请对你的设备负责，也请对你授予 Root 权限的每一个应用负责。

═══════════════════════════════
""".trimIndent()
