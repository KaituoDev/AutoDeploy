package fun.kaituo;

import org.apache.ftpserver.ftplet.*;

public class PluginFtplet extends DefaultFtplet {
    private final AutoDeploy plugin;

    public PluginFtplet(AutoDeploy plugin) {
        this.plugin = plugin;

    }

    @Override
    public void init(FtpletContext context) {
        plugin.getLogger().info("Ftplet initializing");
    }

    @Override
    public void destroy() {
        plugin.getLogger().info("Ftplet destroying");
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) {
        String fileName = request.getArgument();

        plugin.getPluginNamesBuffer().add(fileName);
        return FtpletResult.DEFAULT;
    }
}
