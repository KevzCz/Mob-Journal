// net.pixeldreamstudios.journal.data.MobStat.java
package net.pixeldreamstudios.journal.data;

public record MobStat(int kills, int deaths) {
    public MobStat incrementKills() {
        return new MobStat(kills + 1, deaths);
    }

    public MobStat incrementDeaths() {
        return new MobStat(kills, deaths + 1);
    }

}
