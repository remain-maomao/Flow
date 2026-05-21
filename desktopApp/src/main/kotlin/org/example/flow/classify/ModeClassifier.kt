package org.example.flow.classify

import org.example.flow.model.ActiveWindow
import org.example.flow.model.BrowserMessage
import org.example.flow.model.ClassificationResult
import org.example.flow.model.Mode

/**
 * 模式分类器：根据窗口/浏览器信息判断工作还是娱乐。
 * 纯函数，无状态。默认返回 WORK，只维护娱乐黑名单。
 */
object ModeClassifier {

    // ── 娱乐域名黑名单 ────────────────────────────────
    // 浏览器扩展发来的 domain 字段会与这些值做 contains 匹配
    private val entertainmentDomains = listOf(
        // 视频
        "youtube.com", "bilibili.com", "netflix.com",
        "iqiyi.com", "douyin.com", "twitch.tv",
        "douyu.com", "huya.com",
        // 社交
        "reddit.com", "twitter.com", "x.com",
        "weibo.com", "zhihu.com",
        // 游戏
        "steampowered.com", "steamcommunity.com", "epicgames.com",
        // 小说/漫画
        "qidian.com", "jjwxc.net", "bilibilicomics.com",
    )

    // ── 桌面应用标题关键词 ───────────────────────────
    // 当没有浏览器扩展数据时，用桌面窗口的标题做关键词匹配
    private val entertainmentTitleKeywords = listOf(
        // 游戏平台
        "steam", "epic games", "riot", "原神",
        "崩坏", "星穹铁道", "绝区零", "nikke", "蔚蓝档案",
        // 视频/直播
        "bilibili", "抖音", "虎牙", "斗鱼",
        // 小说
        "起点", "晋江", "漫画", "漫畫",
    )

    // ── 公开 API ──────────────────────────────────────

    /** 对浏览器扩展消息分类 */
    fun classifyBrowser(msg: BrowserMessage): ClassificationResult {
        val keyword = entertainmentDomains.firstOrNull { domain ->
            msg.domain.contains(domain, ignoreCase = true)
        }
        return if (keyword != null) {
            ClassificationResult(Mode.ENTERTAINMENT, keyword, System.currentTimeMillis())
        } else {
            ClassificationResult(Mode.WORK, null, System.currentTimeMillis())
        }
    }

    /** 对桌面窗口信息分类 */
    fun classifyWindow(window: ActiveWindow): ClassificationResult {
        val keyword = entertainmentTitleKeywords.firstOrNull { keyword ->
            window.title.contains(keyword, ignoreCase = true)
        }
        return if (keyword != null) {
            ClassificationResult(Mode.ENTERTAINMENT, keyword, System.currentTimeMillis())
        } else {
            ClassificationResult(Mode.WORK, null, System.currentTimeMillis())
        }
    }
}
