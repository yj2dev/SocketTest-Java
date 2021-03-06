package application._origin;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

class ConnectUser extends Thread {
    Server server;
    Socket socket;

    Vector<ConnectUser> allUser;
    Vector<ConnectUser> waitUser;
    Vector<Room> room;

    InputStream in;
    OutputStream out;
    DataInputStream din;
    DataOutputStream dout;

    String message;
    String nickname;
    Room myRoom;

    final String checkNicknameTag = "checkNickname";
    final String createRoomTag = "createRoom";
    final String enterRoomTag = "enterRoom";
    final String leaveRoomTag = "leaveRoom";

    ConnectUser(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;

        allUser = server.userAllList;
        waitUser = server.userWaitList;
        room = server.roomList;
    }

    public void run() {
        try {
            System.out.println("[ Server ] Client is Connected >> " + this.socket.toString());
            System.out.println("[ Server ] Client, Thread info >> " + socket.getRemoteSocketAddress() + ", "
                    + Thread.currentThread().getName());

            in = this.socket.getInputStream();
            out = this.socket.getOutputStream();
            din = new DataInputStream(in);
            dout = new DataOutputStream(out);
            System.out.println("this >> " + this);
            System.out.println("allUser >> " + allUser);



            while (true) {
//                byte[] buffer = new byte[512];
//                int length = in.read(buffer);
//                while (length == -1) throw new IOException();
                message = din.readUTF();
                //                message = new String(buffer, 0, length, "UTF-8");
                System.out.println("[ Receive ] Succeed >> " + message);

                if (message.contains("@@payload:")) {
                    String payload[] = message.replace("@@payload:##", "").split("##");
                    printPayload(payload);

                    switch (payload[0]) {
                        case checkNicknameTag:
                            System.out.println("[ Server ] ????????? ?????? ??????");
                            //payload[] >> [0]checkNickname, [1]nickname
                            checkNickname(payload);
                            break;
                        case createRoomTag:
                            System.out.println("[ Server ] ??? ??????");
                            //payload[] >> [0]createRoom, [1]roomTitle
                            createRoom(payload);
                            break;
                        case enterRoomTag:
                            System.out.println("[ Server ] ??? ??????");
                            //payload[] >> [0]enterRoom, [1]roomTitle
                            enterRoom(payload);
                            break;
                        case leaveRoomTag:
                            System.out.println("[ Server ] ??? ??????");
                            //payload[] >> [0]leaveRoom, [1]roomTitle
                            leaveRoom(payload);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[ Receive ] Failed >> " + e.toString());
        }
    }





    void sendWaitRoom(String str){
        for(int i = 0; i < waitUser.size(); i++){
            try{
                waitUser.get(i).out.write(str.getBytes(StandardCharsets.UTF_8));
            } catch(Exception e){
                waitUser.remove(i--);
            }
        }
    }

    void sendGameRoom(String str){
        for(int i = 0; i < myRoom.connectUsers.size(); i++){
            try{
                myRoom.connectUsers.get(i).out.write(str.getBytes(StandardCharsets.UTF_8));
            } catch(Exception e){
                myRoom.connectUsers.remove(i--);
            }
        }
    }


    
    void disconnectClient(){
        allUser.remove(this);
        waitUser.remove(this);
    }

    void enterRoom(String payload[]){
        for(int i = 0; i < room.size(); i++){
            if(payload[1].equals(room.get(i).title)) {
                if (room.get(i).userCnt < 2) {
                    System.out.println("[ Server ] ??? ?????? >> Succeed");

                    System.out.println();
                    myRoom = room.get(i);
                    myRoom.userCnt++;

                    waitUser.remove(this);
                    myRoom.connectUsers.add(this);

                    sendWaitRoom(checkRoomList());
                    sendGameRoom(checkRoomUser());

                    send("@@payload:##" + enterRoomTag + "##true" + "##null");
                } else {
                    System.out.println("[ Server ] ??? ?????? >> Failed: ????????? ??????");
                    send("@@payload:##" + enterRoomTag + "##false" + "##????????? ??????????????????.");
                }
            } else {
                System.out.println("[ Server ] ??? ?????? >> Failed: ???????????? ?????? ???");
                send("@@payload:##" + enterRoomTag + "##false" + "##?????? ???????????? ????????????.");

            }
        }
    }

    void leaveRoom(String payload[]){
        myRoom.connectUsers.remove(this);
        myRoom.userCnt--;
        waitUser.add(this);
        System.out.println("[ Server ] ??? ?????? >> Succeed: " + myRoom.title);
        if(myRoom.userCnt < 1){
            room.remove(myRoom);
            System.out.println("[ Server ] ??? ?????? >> " + myRoom.title + "??? ????????? ???????????? ?????? ?????? ??????????????????.");
        }
        if(room.size() > 0){
            sendGameRoom(checkRoomUser());
        }
    }

    void createRoom(String payload[]){
        myRoom = new Room();
        myRoom.userCnt++;
        myRoom.title = payload[1];
        room.add(myRoom);
        myRoom.connectUsers.add(this);
        waitUser.remove(this);
        System.out.println("[ Server ] ??? ?????? >> Succeed");
    }

    void checkNickname(String payload[]) {
        boolean flag = false;
        for(int i = 0; i < allUser.size(); i++){
            if(payload[1].equals(allUser.get(i).nickname)) flag = true;
        }
        if (!flag) {
            nickname = payload[1];
            allUser.add(this);
            waitUser.add(this);
            System.out.println("checkRoomList >> " + checkRoomList());
            sendWaitRoom(checkRoomList());
            System.out.println("[ Server ] ????????? ?????? ?????? >> ????????? ????????????");
            send("@@payload:" + "##checkNickname" + "##false" + "##" + payload[1]);
        } else {
            System.out.println("[ Server ] ????????? ?????? ?????? >> ????????? ????????? ??????");
            send("@@payload:" + "##checkNickname" + "##true" + "##" + payload[1]);
        }
    }

    void printPayload(String str[]) {
        System.out.printf("[ Receive ] Payload[] >> ");
        for (String s : str) {
            System.out.printf("%s ", s);
        }
        System.out.println();
    }

    String checkRoomUser(){
        String str = "@@payload:##" + enterRoomTag;
        for(int i = 0; i < myRoom.connectUsers.size(); i++){
            str += myRoom.connectUsers.get(i).nickname + "##";
        }
        return str;
    }


    String checkRoomList(){
        String str = "@@payload:##" + createRoomTag;
        for(int i = 0; i < room.size(); i ++) {
            str += "##" + room.get(i).title + "##" + room.get(i).userCnt;
        }
        return str;
    }

    void send(String str) {
        try {
            byte[] buffer = str.getBytes(StandardCharsets.UTF_8);
            out.write(buffer);
            System.out.println("[ Send ] Succeed >> " + str);
        } catch (Exception e) {
            System.out.println("[ Send ] Failed >> " + e.toString());
        }
    }

}
