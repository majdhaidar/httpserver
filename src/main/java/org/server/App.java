package org.server;

import org.server.webserver.WebServer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        int serverPort = 8080;
        if(args.length > 0) {
            serverPort = Integer.parseInt(args[0]);
        }

        WebServer webServer = new WebServer(serverPort);
        webServer.start();

        System.out.printf("Server started on port %d\n", serverPort);
    }
}
