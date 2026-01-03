package top.mcmohen.manHunt.rule;

import java.util.List;
import java.util.function.Function;

/**
 * 描述每一项游戏规则的类
 */
public class RuleKey<T> {
    /** 规则名称 */
    private String name;
    /** 规则介绍 */
    private String info;
    /** 规则值的类型 */
    private Class<T> type;
    /** 规则值的描述信息 */
    private String typeInfo;
    /** 规则的推荐值 */
    private List<String> recommendedValues;
    /** String 到 T 的lambda函数 - 成功则返回转换后的值，失败则返回null */
    private Function<String, T> validate;

    // 构建上述6个属性的构造函数
    public RuleKey(
        String name,
        String info,
        Class<T> type,
        String typeInfo,
        List<String> recommendedValues,
        Function<String, T> validate
    ) {
        this.name = name;
        this.info = info;
        this.type = type;
        this.typeInfo = typeInfo;
        this.recommendedValues = recommendedValues;
        this.validate = validate;
    }
    // 使用该构造函数，添加一些静态规则

    // 猎人准备倒计时规则
    public static final RuleKey<Integer> Hunter_Ready_CD = new RuleKey<Integer>(
        "hunter_ready_cd",
        "猎人准备倒计时（秒）",
        Integer.class,
        "整数",
        List.of("0", "10", "20", "30"),
            ((String s) -> {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            try {
                int v = Integer.parseInt(s);
                if (v < 0 || v > 120) return null;
                return (Integer) v;
            } catch (NumberFormatException e) {
                return null;
            }
        })
    );

    // 猎人复活时间规则
    public static final RuleKey<Integer> Hunter_Respawn_CD = new RuleKey<Integer>(
        "hunter_respawn_cd",
        "猎人复活时间（秒）",
        Integer.class,
        "整数",
        List.of("0", "10", "15", "20"),
            ((String s) -> {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            try {
                int v = Integer.parseInt(s);
                if (v < 0 || v > 120) return null;
                return (Integer) v;
            } catch (NumberFormatException e) {
                return null;
            }
        })
    );

    // 是否开启友伤
    public static final RuleKey<Boolean> Friendly_Fire = new RuleKey<Boolean>(
        "friendly_fire",
        "是否开启友伤",
        Boolean.class,
        "布尔值(true/false)",
        List.of("true", "false"),
            ((String s) -> {
            if (s == null) return null;
            s = s.trim().toLowerCase();
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
            return null;
        })
    );


    /**
     * 获取validate函数
     */
    public Function<String, T> getValidate() {
        return validate;
    }

    /**
     * 获取规则名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取规则介绍
     */
    public String getInfo() {
        return info;
    }

    /**
     * 获取规则值的类型
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * 获取规则值的描述信息
     */
    public String getTypeInfo() {
        return typeInfo;
    }

    /**
     * 获取推荐值列表（用于命令提示和帮助）
     */
    public List<String> getRecommendedValues() {
        return recommendedValues;
    }

}
