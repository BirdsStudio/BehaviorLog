package dev.dgteam.behaviorlog;

public class BehaviorLogItem {
    private String time;
    private String player;
    private String position;
    private String behavior;
    private String format;

    public BehaviorLogItem() {
    }

    public BehaviorLogItem(String time, String player, String position, String behavior, String format) {
        this.time = time;
        this.player = player;
        this.position = position;
        this.behavior = behavior;
        this.format = format;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getBehavior() {
        return behavior;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        if (format != null){
            if (!format.isEmpty()){
                return format.replace("@time",time).replace("@p",player).replace("@position",position).replace("@behavior",behavior);
            }
        }
        // The player placed items in one place
        return time + " " + player + " " + behavior + " in " + position;
    }
}
