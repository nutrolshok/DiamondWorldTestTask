package net.nutrolshok.diamondworldtesttask;

import net.nutrolshok.diamondworldtesttask.mysql.MysqlThread;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class MySQLWorker extends MysqlThread {

    public MySQLWorker(@NotNull final ConfigurationSection section) {
        super(Main.getInstance(), new MysqlThread.MysqlConfigSupplier(() -> "jdbc:mysql://" + section.getString("host") +
                ":" + section.getString("port") + "/" + section.getString("database"),
                () -> section.getString("username"),
                () -> section.getString("password")));
        this.useUnicode();
        createTable();

        start();
    }

    private void createTable() {
        this.query("CREATE TABLE IF NOT EXISTS `ocelots` (" +
                "id int(64) NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "player_name varchar(50) NOT NULL," +
                "ocelot_name varchar(50) NOT NULL," +
                "time DATETIME NOT NULL" +
                ")");
    }

    public void insert(@NotNull final String playerName, @NotNull final String ocelotName, @NotNull final String time) {
        this.query("INSERT INTO `ocelots` (player_name,ocelot_name,time) VALUES (" +
                "'" + playerName + "'," +
                "'" + ocelotName + "'," +
                "'" + time + "'" +
                ")");
    }

}
