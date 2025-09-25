package com.returns.lore.utils;

public enum StatType {
    공격력("공격력"),
    방어력("방어력"),
    생명력("생명력"),
    치명타확률("치명타확률"),
    치명타데미지("치명타데미지"),
    흡혈률("흡혈률"),
    흡혈력("흡혈력"),
    회피율("회피율"),
    방어구관통력("방어구관통력"),
    이동속도("이동속도"),
    재생력("재생력");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StatType fromString(String name) {
        for (StatType type : values()) {
            if (type.displayName.equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static String getValidStats() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values().length; i++) {
            sb.append(values()[i].displayName);
            if (i < values().length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}