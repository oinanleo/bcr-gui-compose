package com.leo.bcrgui

import android.content.Context

/**
 * 通话内容对应的图标集合（Material Symbols outlined，已转成 drawable 里的 ic_ms_<name>）。
 * first 是资源名，second 是给 AI 看的中文含义。
 */
object IconSet {
    val groups: List<Pair<String, List<Pair<String, String>>>> = listOf(
        "学习 · 作业" to listOf(
            "home_work" to "作业", "assignment" to "作业单", "menu_book" to "课本", "book" to "书",
            "auto_stories" to "阅读", "library_books" to "资料", "local_library" to "图书馆", "school" to "学校",
            "history_edu" to "历史", "science" to "实验", "calculate" to "算术", "functions" to "数学",
            "architecture" to "画图", "engineering" to "工程", "biotech" to "生物", "language" to "语文外语",
            "translate" to "翻译", "edit_note" to "记笔记", "quiz" to "考试", "lightbulb" to "灵感",
            "psychology" to "思考", "code" to "编程", "terminal" to "命令行", "computer" to "电脑",
            "emoji_objects" to "创意", "draw" to "画画", "palette" to "美术", "spellcheck" to "背单词",
            "mic" to "朗读录音", "sticky_note_2" to "便签", "note_add" to "新建笔记", "history" to "历史记录",
        ),
        "家人 · 生活" to listOf(
            "family_restroom" to "家人", "group" to "亲友", "groups" to "家庭群", "diversity_3" to "一家人",
            "elderly" to "长辈", "child_care" to "小孩", "child_friendly" to "带娃", "pregnant_woman" to "孕期",
            "pets" to "宠物", "favorite" to "关心", "home" to "家里", "house" to "房子",
            "apartment" to "小区", "kitchen" to "做饭", "restaurant" to "吃饭", "local_dining" to "聚餐",
            "ramen_dining" to "面食", "dinner_dining" to "晚饭", "cake" to "生日", "celebration" to "庆祝",
            "icecream" to "甜品", "local_cafe" to "咖啡奶茶", "local_laundry_service" to "洗衣", "cleaning_services" to "打扫",
            "soap" to "洗护", "bed" to "睡觉", "wash" to "洗手", "chair" to "家具",
            "breakfast_dining" to "早餐", "soup_kitchen" to "煲汤",
        ),
        "朋友 · 娱乐" to listOf(
            "sports_esports" to "游戏", "stadia_controller" to "手柄", "videogame_asset" to "电子游戏", "sports_basketball" to "篮球",
            "sports_soccer" to "足球", "sports_tennis" to "网球", "sports_volleyball" to "排球", "sports_gymnastics" to "体操",
            "sports_martial_arts" to "武术", "golf_course" to "高尔夫", "pool" to "游泳", "fitness_center" to "健身",
            "directions_bike" to "骑行", "hiking" to "徒步", "camping" to "露营", "beach_access" to "海边",
            "park" to "公园", "movie" to "电影", "theaters" to "影院", "live_tv" to "看电视",
            "tv" to "追剧", "music_note" to "音乐", "headphones" to "耳机", "library_music" to "歌单",
            "radio" to "电台", "podcasts" to "播客", "casino" to "娱乐", "playing_cards" to "打牌",
            "chess" to "下棋", "attractions" to "游乐园", "festival" to "展会", "nightlife" to "夜生活",
            "confirmation_number" to "门票", "luggage" to "旅行", "photo_camera" to "拍照", "videocam" to "拍视频",
            "party_mode" to "派对", "album" to "相册", "sports_bar" to "酒吧", "liquor" to "喝酒",
            "surfing" to "冲浪", "kayaking" to "皮划艇",
        ),
        "出行 · 买买买" to listOf(
            "local_taxi" to "打车", "directions_car" to "开车", "car_repair" to "修车", "local_parking" to "停车",
            "local_gas_station" to "加油", "two_wheeler" to "电动车", "electric_scooter" to "共享单车", "directions_bus" to "公交",
            "subway" to "地铁", "train" to "火车", "flight" to "飞机", "hotel" to "酒店",
            "local_shipping" to "快递", "delivery_dining" to "外卖", "takeout_dining" to "打包", "local_grocery_store" to "买菜",
            "shopping_cart" to "网购", "shopping_bag" to "商场", "storefront" to "店铺", "store" to "小店",
            "local_mall" to "买衣服", "inventory" to "库存", "warehouse" to "仓库", "factory" to "工厂",
            "real_estate_agent" to "中介看房", "move_to_inbox" to "搬家", "local_offer" to "优惠", "percent" to "折扣",
            "local_car_wash" to "洗车", "ev_station" to "充电桩",
        ),
        "维修 · 社区" to listOf(
            "plumbing" to "水管", "electrical_services" to "电工", "handyman" to "上门维修", "build" to "装修",
            "carpenter" to "木工", "door_front" to "门锁", "key" to "钥匙", "lock" to "密码锁",
            "local_police" to "警察", "local_fire_department" to "消防", "emergency" to "急救", "security" to "安保",
            "water_drop" to "漏水", "bolt" to "电", "local_pharmacy" to "药店", "medication" to "吃药",
            "medical_services" to "看病", "local_hospital" to "医院", "vaccines" to "疫苗", "health_and_safety" to "体检",
            "spa" to "养生", "content_cut" to "理发", "face" to "面部护理", "badge" to "证件",
        ),
        "钱 · 工作 · 通知" to listOf(
            "account_balance" to "银行", "account_balance_wallet" to "钱包", "payments" to "转账缴费", "credit_card" to "还款",
            "receipt_long" to "发票", "receipt" to "小票", "loyalty" to "会员卡", "request_quote" to "报价",
            "savings" to "存钱", "local_atm" to "取现", "work" to "工作", "business_center" to "商务",
            "work_history" to "面试", "handshake" to "合作", "campaign" to "推销", "support_agent" to "客服",
            "phone_in_talk" to "通话", "headset_mic" to "客服耳机", "description" to "合同", "folder" to "文件",
            "task" to "待办", "event" to "预约", "schedule" to "日程", "calendar_month" to "日历",
            "alarm" to "闹钟", "timer" to "计时", "priority_high" to "紧急", "report" to "投诉",
            "gpp_maybe" to "疑似诈骗", "block" to "拉黑", "notifications" to "提醒", "email" to "邮件",
            "sms" to "短信", "chat" to "聊天", "wifi" to "网络", "router" to "宽带路由",
            "phone_iphone" to "手机", "print" to "打印", "qr_code_scanner" to "扫码", "contact_phone" to "通讯录",
            "contacts" to "联系人", "public" to "海外",
        ),
    )

    val choices: List<Pair<String, String>> = groups.flatMap { it.second }
    val names: List<String> = choices.map { it.first }

    /** 长的名字先试，避免 "book" 抢了 "menu_book"。 */
    private val byLength: List<String> = names.sortedByDescending { it.length }

    fun isName(s: String) = s in names

    /** 从模型输出里挑一个合法图标名，挑不到返回空串。 */
    fun match(raw: String): String {
        val s = raw.lowercase()
        names.firstOrNull { it == s.trim() }?.let { return it }
        return byLength.firstOrNull { s.contains(it) } ?: ""
    }

    /** 给 AI 的清单：按场景分组，一行一组。 */
    val prompt: String = groups.joinToString("\n") { (title, items) ->
        "【" + title + "】" + items.joinToString(", ") { it.first + it.second }
    }

    private val cache = HashMap<String, Int>()

    /** 名字 → drawable id，找不到返回 0。 */
    fun res(ctx: Context, name: String): Int = cache.getOrPut(name) {
        ctx.resources.getIdentifier("ic_ms_$name", "drawable", ctx.packageName)
    }
}
