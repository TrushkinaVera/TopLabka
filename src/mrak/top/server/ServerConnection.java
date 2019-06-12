package mrak.top.server;

import mrak.top.Command;
import mrak.top.Pair;
import mrak.top.User;
import mrak.top.connection.Header;
import mrak.top.connection.Packet;
import mrak.top.connection.ResponseCode;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class ServerConnection implements Runnable{
    private Semaphore smp;
    private Socket socket;
    public ServerConnection(Semaphore smp, Socket socket) {
        System.out.println("new connection detected");
        this.smp = smp;
        this.socket = socket;
        new Thread(this).start();
    }
    @Override
    public void run() {

        try {
            System.out.println("waiting for input");
            OutputStream ts = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            //Debugger for starnge requests
            /*Scanner d = new Scanner(is);
            while(true){
                if(d.hasNext()){
                    System.out.println(d.nextLine());
                }
            }*/
            ObjectOutputStream oos = new ObjectOutputStream(ts);
            ObjectInputStream ois = new ObjectInputStream(is);
            Packet input;
            while(true){
                if((input = (Packet) ois.readObject()) != null){
                    //блокиров очка
                    smp.acquire();

                    User authData = input.getUser();
                    //вот твой инпут пакет
                    Command cmd = input.getCommand();
                    System.out.println(input.getCommand().getText());
                    if(ServerMain.auth(ServerMain.conn, authData.getLogin(), authData.getPassword()) || "login".equals(cmd.getText()) || "register".equals(cmd.getText())) {
                        System.out.println(input.getCommand().getText());
                        //работаем с чем нам надо
                        Object razvrat = null;
                        boolean executed = false;
                        try {
                            for (CollectionCommand e : ServerMain.cmds) {
                                if (e.getName().equals(cmd.getText())) {
                                    razvrat = e.doCommand(ServerMain.conn, cmd.getArgument(), input.getUser());
                                    executed = true;
                                    break;
                                }
                            }
                            if (executed != true) {
                                razvrat = "Command not found";
                            }
                        } catch (SQLException e) {
                            //TODO: todo say client about
                            razvrat = "Error while executing SQL";
                            e.printStackTrace();
                        }

                        Packet response = Packet.formPacket(new Pair<>(Header.CODE, razvrat != null ? ResponseCode.OK : ResponseCode.BAD_REQUEST), new Pair<>(Header.DATA, razvrat));
                        oos.writeObject(response);
                        oos.flush();
                    }

                    smp.release();
                    //передер
                }
            }
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        } catch (EOFException exception) {
            System.out.println("client disconnected");
        } catch (IOException ex) {
            System.out.println("Кажется, мы сломались");
            ex.printStackTrace();
            System.exit(0);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
