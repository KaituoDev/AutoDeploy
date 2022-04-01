package fun.kaituo;

import com.rylinaux.plugman.event.PluginLoadEvent;
import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.RED;

public class AutoDeploy extends JavaPlugin implements Listener {
    int port;
    int passivePortsStart;
    int passivePortsEnd;
    String passiveExternalAddress;
    FtpServer server;
    Set<String> fileNames;
    Set<String> fileNamesBuffer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration c = getConfig();
        port = c.getInt("bind-port");
        passiveExternalAddress = c.getString("passive-external-address");
        passivePortsStart = c.getInt("passive-ports-start");
        passivePortsEnd = c.getInt("passive-ports-end");
        fileNames = new HashSet<>();
        fileNamesBuffer = new HashSet<>();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("adclear").setExecutor(this);
        try {
            DataConnectionConfigurationFactory dataConnectionConfigurationFactory = new DataConnectionConfigurationFactory();
            dataConnectionConfigurationFactory.setPassivePorts(passivePortsStart + "-" + passivePortsEnd);
            dataConnectionConfigurationFactory.setPassiveExternalAddress(passiveExternalAddress);
            dataConnectionConfigurationFactory.setActiveEnabled(false);
            DataConnectionConfiguration dataConnectionConfiguration = dataConnectionConfigurationFactory.createDataConnectionConfiguration();
            FtpServerFactory serverFactory = new FtpServerFactory();
            ListenerFactory listenerFactory = new ListenerFactory();
            listenerFactory.setPort(port);
            listenerFactory.setDataConnectionConfiguration(dataConnectionConfiguration);
            org.apache.ftpserver.listener.Listener listener = listenerFactory.createListener();

            serverFactory.addListener("default", listener);
            BaseUser user = new BaseUser();
            user.setName(c.getString("username"));
            user.setPassword(c.getString("password"));
            user.setHomeDirectory("/home/yfshadaow/servers/minigame/plugins/");
            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);
            serverFactory.getUserManager().save(user);
            Map<String, Ftplet> ftpletMap = new HashMap<>();
            ftpletMap.put("ftplet", new PluginFtplet(this));
            serverFactory.setFtplets(ftpletMap);
            server = serverFactory.createServer();
            server.start();
            getLogger().info("Ftp server started on port " + port);
        } catch (FtpException e) {
            e.printStackTrace();
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            StringBuilder msg = new StringBuilder("§b以下插件需要加载/重载： ");
            if (fileNames.isEmpty()) {
                return;
            } else for (String fileName : fileNames) {
                msg.append("§f").append(fileName).append(", ");
            }
            msg.delete(msg.length() - 2, msg.length() - 1);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("autodeploy.notify")) {
                    p.sendMessage(msg.toString());
                }
            }
        }, 3600, 3600);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ()-> {
            for (String name: fileNamesBuffer) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("autodeploy.notify")) {
                        p.sendMessage("§f" + name + "§b需要加载/重载！");
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 1, 1);
                    }
                }
            }
            fileNamesBuffer.clear();
        },1,1);
    }

    @Override
    public void onDisable() {
        server.stop();
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((Plugin) this);
    }

    @EventHandler
    public void onPluginLoad(PluginLoadEvent event) {
        Bukkit.broadcastMessage(event.getFileName());
        fileNames.remove(event.getFileName());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("adclear")) {
            if (sender instanceof Player) {
                if (sender.isOp()) {
                    fileNames.clear();
                    sender.sendMessage(AQUA + "已清空未处理插件列表");
                } else {
                    sender.sendMessage(RED + "你没有权限执行这个指令！");
                }
            } else {
                sender.sendMessage(RED + "此指令只能由玩家执行！");
            }
        }
        return true;
    }
}
