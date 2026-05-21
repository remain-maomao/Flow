package org.example.flow.classify

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 应用配置管理器：持久化娱乐黑名单到用户目录下的 JSON 文件。
 */
object ConfigManager {

    private val configDir = File(System.getProperty("user.home"), ".flow")
    private val configFile = File(configDir, "config.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true  // 确保默认值也被写入 JSON
    }

    /** 加载配置，文件不存在时返回默认值并自动创建 */
    fun load(): AppConfig {
        println("[ConfigManager] Loading config...")
        return try {
            if (configFile.exists()) {
                val content = configFile.readText()
                println("[ConfigManager] File exists, size=${content.length}")
                val cfg = json.decodeFromString<AppConfig>(content)
                println("[ConfigManager] Loaded: domains=${cfg.entertainmentDomains.size}, apps=${cfg.entertainmentApps.size}")
                cfg
            } else {
                println("[ConfigManager] File not found, creating defaults")
                val default = AppConfig()
                save(default)
                default
            }
        } catch (e: Exception) {
            println("[ConfigManager] Load failed: ${e.message}")
            e.printStackTrace()
            AppConfig()
        }
    }

    /** 保存配置到磁盘 */
    fun save(config: AppConfig) {
        try {
            if (!configDir.exists()) configDir.mkdirs()
            val jsonStr = json.encodeToString(AppConfig.serializer(), config)
            println("[ConfigManager] Writing JSON (${jsonStr.length} bytes): domains=${config.entertainmentDomains.size}, apps=${config.entertainmentApps.size}")
            configFile.writeText(jsonStr)
            println("[ConfigManager] Saved to ${configFile.absolutePath}")
        } catch (e: Exception) {
            println("[ConfigManager] Save failed: ${e.message}")
            e.printStackTrace()
        }
    }
}

/**
 * 应用配置数据。
 * @param entertainmentDomains 娱乐域名黑名单
 * @param entertainmentApps 娱乐应用标题关键词
 */
@Serializable
data class AppConfig(
    val entertainmentDomains: List<String> = DEFAULT_DOMAINS,
    val entertainmentApps: List<String> = DEFAULT_APPS,
) {
    companion object {
        val DEFAULT_DOMAINS = listOf(
            "youtube.com", "bilibili.com", "netflix.com",
            "iqiyi.com", "douyin.com", "twitch.tv",
            "douyu.com", "huya.com", "reddit.com",
            "twitter.com", "x.com", "weibo.com", "zhihu.com",
            "steampowered.com", "steamcommunity.com", "epicgames.com",
            "qidian.com", "jjwxc.net",
        )

        val DEFAULT_APPS = listOf(
            "steam", "epic games", "riot", "原神",
            "崩坏", "星穹铁道", "绝区零", "nikke", "蔚蓝档案",
            "bilibili", "抖音", "虎牙", "斗鱼",
            "起点", "晋江", "漫画", "漫畫",
        )
    }
}
