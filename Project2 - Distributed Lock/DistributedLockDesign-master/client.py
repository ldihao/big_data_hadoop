"""
my_client:
Connecting with leader_server or follower_server.
A client can preempt lock, release lock and check lock.
And the server will response these three actions
"""
from tkinter import *
from socket import *
import datetime
import threading

class Client():

    def __init__(self, server_port):
        self.port = server_port
        self.client_id = None
        self.socket = None

    def _connect_with_server(self, hostname, server_port):
        print("start to connect with server")
        server_socket = socket(AF_INET, SOCK_STREAM)
        server_socket.connect((hostname, server_port))
        self.socket = server_socket
        senddata = "NewClient"
        server_socket.sendall(senddata.encode())
        print("client send data")
        data = server_socket.recv(1024).decode('utf-8')
        msg = data.split(":")
        if msg[0] == "ClientId":
            self.client_id = msg[1]
        return server_socket, self.client_id

    def preemptlock(self,lockname):
        # send data form: "PreemptLock:Lock:client_id"
        send_data = "PreemptLock:"+lockname+":"+self.client_id
        self.socket.sendall(send_data.encode())
        time1 = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(time1," Request: preemptlock")
        data = self.socket.recv(1024).decode('utf-8')
        time2 = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(time2," Response: ", data)

    def releaselock(self,lockname):
        # send data form: "ReleaseLock:Lock:client_id"
        send_data = "ReleaseLock:"+lockname+":"+self.client_id
        self.socket.sendall(send_data.encode())
        time1 = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(time1," Request: releaselock")
        data = self.socket.recv(1024).decode('utf-8')
        time2 = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(time2," Response: ", data)

    def checklock(self,lockname):
        # send data form: "CheckLock:Lock:client_id"
        send_data = "CheckLock:"+lockname+":"+self.client_id
        self.socket.sendall(send_data.encode())
        time1 = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(time1," Request: checklock")
        data = self.socket.recv(1024).decode('utf-8')
        time2 = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(time2," Response: ", data)


    def request(self):
        while True:
            command = input("distributeLock:> ")
            command = command.split(' ')
            if command[0] == 'preempt':
                self.preemptlock(command[1])
            elif command[0] == 'release':
                self.releaselock(command[1])
            elif command[0] == 'check':
                self.checklock(command[1])
            else:
                print("Wrong command!\n")



    def run(self):
        hostname = '127.0.0.1'
        port = self.port
        server_socket, client_id = self._connect_with_server(hostname,port)
        print("server socket:", server_socket)
        print("client_id:", client_id)
        self.request()


# client = My_Client(9000)
# client.run()


