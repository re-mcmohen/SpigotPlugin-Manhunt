package top.mcmohen.manHunt.rule;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 描述所有游戏规则的类
 */
public class GameRules {
    private Map<RuleKey<?>, Object> map = new LinkedHashMap<>();

     public GameRules() {
         // 设置默认规则值
         setRule(RuleKey.Hunter_Ready_CD, 30);
         setRule(RuleKey.Friendly_Fire, true);
         setRule(RuleKey.Hunter_Respawn_CD, 15);
     }

    /**
     * 设置规则对应的值
     */
    private <T> void setRule(RuleKey<T> key, T value) {
        map.put(key, value);
    }

    /**
     * 为规则设置值(从String安全转换)
     */
    public <T> boolean setGameRuleValueSafe (RuleKey<T> rule, String value) {
        T okValue = rule.getValidate().apply(value);
        if (okValue != null) {
            setRule(rule, okValue);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取规则对应的值
     */
    public <T> T getRuleValue(RuleKey<T> rule) {
        var value = map.get(rule);
        if (value != null)
            return (T) value;
        else
            return null;
    }

    /**
     * 展示所有游戏规则
     */
    public Map<RuleKey<?>, Object> getAllRules() {
        return map;
    }

}
