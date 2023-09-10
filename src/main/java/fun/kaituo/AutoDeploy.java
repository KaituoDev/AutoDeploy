package fun.kaituo;

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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class AutoDeploy extends JavaPlugin implements Listener {
    private FtpServer server;
    private final Set<String> pluginNamesBuffer = new HashSet<>();

    public Set<String> getPluginNamesBuffer() {
        return pluginNamesBuffer;
    }
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration c = getConfig();
        int port = c.getInt("bind-port");
        String passiveExternalAddress = c.getString("passive-external-address");
        int passivePortsStart = c.getInt("passive-ports-start");
        int passivePortsEnd = c.getInt("passive-ports-end");
        String homeDirectory = c.getString("home-directory");
        Bukkit.getPluginManager().registerEvents(this, this);
        try {
            DataConnectionConfiguration dataConnectionConfiguration = getDataConnectionConfiguration(passivePortsStart, passivePortsEnd, passiveExternalAddress);
            FtpServerFactory serverFactory = new FtpServerFactory();
            ListenerFactory listenerFactory = new ListenerFactory();
            listenerFactory.setPort(port);
            listenerFactory.setDataConnectionConfiguration(dataConnectionConfiguration);
            org.apache.ftpserver.listener.Listener listener = listenerFactory.createListener();

            serverFactory.addListener("default", listener);
            BaseUser user = new BaseUser();
            user.setName(c.getString("username"));
            user.setPassword(c.getString("password"));
            user.setHomeDirectory(homeDirectory);
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
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ()-> {
            for (String name: pluginNamesBuffer) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("autodeploy.notify")) {
                        p.sendMessage("§f" + name + "§b需要加载/重载！");
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 1, 1);
                    }
                }
                getLogger().info("§f" + name + "§b需要加载/重载！");
            }
            pluginNamesBuffer.clear();
        },1,1);
    }

    private DataConnectionConfiguration getDataConnectionConfiguration(int passivePortsStart, int passivePortsEnd, String passiveExternalAddress) {
        DataConnectionConfigurationFactory dataConnectionConfigurationFactory = new DataConnectionConfigurationFactory();
        dataConnectionConfigurationFactory.setPassivePorts(passivePortsStart + "-" + passivePortsEnd);
        dataConnectionConfigurationFactory.setPassiveExternalAddress(passiveExternalAddress);
        dataConnectionConfigurationFactory.setActiveEnabled(false);
        return dataConnectionConfigurationFactory.createDataConnectionConfiguration();
    }

    @Override
    public void onDisable() {
        server.stop();
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((Plugin) this);
    }

}
