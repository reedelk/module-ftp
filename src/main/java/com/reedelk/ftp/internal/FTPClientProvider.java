package com.reedelk.ftp.internal;

import com.reedelk.ftp.component.ConnectionConfiguration;
import com.reedelk.ftp.internal.commons.Default;
import com.reedelk.runtime.api.component.Implementor;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;

import static com.reedelk.ftp.component.ConnectionType.FTPS;
import static com.reedelk.runtime.api.commons.ConfigurationPreconditions.requireNotBlank;
import static com.reedelk.runtime.api.commons.ConfigurationPreconditions.requireNotNull;
import static java.util.Optional.ofNullable;

public class FTPClientProvider {

    private final int port;
    private final String host;
    private final String username;
    private final String password;

    private FTPClient ftp;

    public FTPClientProvider(Class<? extends Implementor> implementor, ConnectionConfiguration connection) {
        requireNotNull(implementor, connection, "FTP Connection Configuration must be provided.");
        requireNotBlank(implementor, connection.getHost(), "FTP Connection host must not be empty.");
        requireNotBlank(implementor, connection.getUsername(), "FTP Connection username must not be empty.");
        requireNotBlank(implementor, connection.getPassword(), "FTP Connection password must not be empty.");

        port = ofNullable(connection.getPort()).orElse(Default.FTP_PORT);
        host = connection.getHost();
        username = connection.getUsername();
        password = connection.getPassword();

        ftp = FTPS.equals(connection.getType()) ?
                new FTPSClient() : new FTPClient();
    }

    public <T> T execute(Command<T> command, ExceptionMapper exceptionMapper) {
        try {

            open(exceptionMapper);

            return command.execute(ftp);

        } catch (IOException exception) {

            throw exceptionMapper.from(exception);

        } finally {

            close();

        }
    }

    private void close() {
        try {
            ftp.logout();
        } catch (IOException e) {
            // Nothing we can do here. We silently fail.
        }
        try {
            ftp.disconnect();
        } catch (IOException exception) {
            // Nothing we can do here. We silently fail.
        }
    }

    private void open(ExceptionMapper exceptionMapper) throws IOException {
        ftp.connect(host, port);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw exceptionMapper.from("Could not open FTP connection. Reply code (%d) was not successful.");
        }
        boolean login = ftp.login(username, password);
        if (!login) {
            throw exceptionMapper.from("Could not login! Username and password wrong?");
        }
    }

    public void dispose() {
        close();
    }
}
