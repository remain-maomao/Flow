package org.example.flow.classify

import org.example.flow.model.ActiveWindow
import org.example.flow.model.BrowserMessage
import org.example.flow.model.ClassificationResult
import org.example.flow.model.Mode

/**
 * 模式分类器：根据窗口/浏览器信息判断工作还是娱乐。
 * 从 ConfigManager 读取黑名单（支持用户自定义）。
 */
object ModeClassifier {

    private var cachedConfig: AppConfig? = null

    /** 刷新配置缓存（用户在设置面板修改后调用） */
    fun reload() {
        cachedConfig = ConfigManager.load()
        println("[ModeClassifier] Config reloaded: ${cachedConfig?.entertainmentDomains?.size} domains, ${cachedConfig?.entertainmentApps?.size} apps")
    }

    private fun getConfig(): AppConfig {
        if (cachedConfig == null) reload()
        return cachedConfig!!
    }

    /** 对浏览器扩展消息分类 */
    fun classifyBrowser(msg: BrowserMessage): ClassificationResult {
        val config = getConfig()
        val url = msg.url
        val domain = msg.domain

        // 1. Whitelist check
        val whitelistHit = config.whitelistUrls.firstOrNull { pattern ->
            url.contains(pattern, ignoreCase = true)
        }
        if (whitelistHit != null) {
            println("[Classifier] BROWSER url=$url -> WHITELIST hit='$whitelistHit' -> WORK")
            return ClassificationResult(Mode.WORK, "whitelist:$whitelistHit", System.currentTimeMillis())
        }

        // 2. Blacklist check
        val blackHit = config.entertainmentDomains.firstOrNull { d ->
            domain.contains(d, ignoreCase = true)
        }
        if (blackHit != null) {
            println("[Classifier] BROWSER url=$url domain=$domain -> BLACKLIST hit='$blackHit' -> ENTERTAINMENT")
            return ClassificationResult(Mode.ENTERTAINMENT, blackHit, System.currentTimeMillis())
        }

        // 3. Default
        println("[Classifier] BROWSER url=$url domain=$domain -> default -> WORK")
        return ClassificationResult(Mode.WORK, null, System.currentTimeMillis())
    }

    /** 对桌面窗口信息分类 */
    fun classifyWindow(window: ActiveWindow): ClassificationResult {
        val config = getConfig()
        val title = window.title
        val keyword = config.entertainmentApps.firstOrNull { keyword ->
            title.contains(keyword, ignoreCase = true)
        }
        return if (keyword != null) {
            println("[Classifier] WINDOW title='$title' process=${window.processName} -> HIT '$keyword' -> ENTERTAINMENT")
            ClassificationResult(Mode.ENTERTAINMENT, keyword, System.currentTimeMillis())
        } else {
            println("[Classifier] WINDOW title='$title' process=${window.processName} -> no match -> WORK")
            ClassificationResult(Mode.WORK, null, System.currentTimeMillis())
        }
    }
}
