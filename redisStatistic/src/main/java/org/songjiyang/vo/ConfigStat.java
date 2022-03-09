package org.songjiyang.vo;

public class ConfigStat {

    private String type;

    private Integer num;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "ConfigStat{" +
                "type='" + type + '\'' +
                ", num=" + num +
                '}';
    }
}
