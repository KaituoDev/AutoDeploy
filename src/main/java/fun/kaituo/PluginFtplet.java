package fun.kaituo;

import org.apache.ftpserver.ftplet.*;

import java.util.Set;

public class PluginFtplet extends DefaultFtplet {
    Set<String> fileNames;
    Set<String> fileNameBuffer;

    public PluginFtplet(AutoDeploy plugin) {
        this.fileNames = plugin.fileNames;
        this.fileNameBuffer = plugin.fileNamesBuffer;

    }

    @Override
    public void init(FtpletContext context) {
        System.out.println("Ftplet initializing");
    }

    @Override
    public void destroy() {
        System.out.println("Ftplet destroying");
    }

    /*
    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request) {
        System.out.println("yeah!");
        return FtpletResult.DEFAULT;
    }

     */
    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) {
        String fileName = request.getArgument();
        fileNames.add(fileName);
        fileNameBuffer.add(fileName);
        return FtpletResult.DEFAULT;
    }
}
