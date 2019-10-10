package Main;

import Node.DataNode;
import Node.NameNode;
import Tools.Operation;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;

public class Main {
	NameNode nameNode;
	DataNode[] dataNode;

	public void init() {
		Manager.init();
		// 创建文件夹"dfs",若不存在
		File dfsDir = new File("dfs");
		if (!dfsDir.exists())
			dfsDir.mkdir();

		// 创建namenode,datanodes文件夹
		File nameDir = new File("dfs/namenode");
		if (!nameDir.exists())
			nameDir.mkdir();
		File dataDir;
		for (int i = 0; i < 4; i++) {
			dataDir = new File("dfs/datanode" + i);
			if (!dataDir.exists())
				dataDir.mkdir();
		}

		// 创建线程并启动
		nameNode = new NameNode();
		nameNode.start();

		dataNode = new DataNode[4];
		for (int i = 0; i < 4; i++) {
			dataNode[i] = new DataNode(i);
			dataNode[i].start();
		}
	}

	public void start() throws BrokenBarrierException, InterruptedException {
		// shell
		Scanner scan = new Scanner(System.in);
		while (true) {
			System.out.print("MiniDFS > ");
			Manager.cmd_flag = processCmd(scan.nextLine());
			if (Manager.cmd_flag) {
				Manager.name_event.await();
				if (Manager.cmd_type == Operation.quit) {
					scan.close();
					System.exit(0);
				} else if (Manager.cmd_type == Operation.put) {
					for (int i = 0; i < Manager.SERVER_NUMBER; i++)
						Manager.main_event[i].await();
					System.out.println("Upload success!");
				} else if (Manager.cmd_type == Operation.read)
					Manager.read_event.await();

				else if (Manager.cmd_type == Operation.ls)
					Manager.ls_event.await();
				else if (Manager.cmd_type == Operation.fetch) {
					Manager.main_event[0].await();
					System.out.println("Fetch success!");
				} else if (Manager.cmd_type == Operation.recover) {
					for (int i = 0; i < Manager.SERVER_NUMBER; i++)
						Manager.main_event[i].await();
					
				} else
					continue;
			}
//            System.out.println("pass");
		}
	}

	public boolean processCmd(String s) {
		String[] cmds = s.split(" ");
		if (cmds[0].equals("quit"))
			if (cmds.length != 1) {
				System.out.println("quit Usage: quit");
				return false;
			} else {
				Manager.cmd_type = Operation.quit;
			}
		else if (cmds[0].equals("ls")) {
			if (cmds.length > 1) {
				System.out.println("ls Usage: ls");
				return false;
			} else {
				Manager.cmd_type = Operation.ls;
			}
		} else if (cmds[0].equals("put")) {
			if (cmds.length != 2) {
				System.out.println("put Usage: put file_path");
				return false;
			} else {
				if (!new File(cmds[1]).exists()) {
					System.out.println("File not exist.Please check your file_path");
					return false;
				} else {
					Manager.cmd_type = Operation.put;
					Manager.file_path = cmds[1];
				}
			}
		} else if (cmds[0].equals("read")) {
			if (cmds.length != 2) {
				System.out.println("read Usage: read file_id");
				return false;
			} else {
				try {
					Manager.file_ID = Integer.parseInt(cmds[1]);
				} catch (NumberFormatException e) {
					System.out.println("file_id must be integer.");
					return false;
				}
				Manager.cmd_type = Operation.read;
				return true;
			}
		} else if (cmds[0].equals("fetch")) {
			if (cmds.length != 3) {
				System.out.println("fetch Usage: fetch file_id save_path");
				return false;
			} else {
				if (!new File(cmds[2]).isDirectory()) {
					System.out.println("Invalid save_path.");
					return false;
				}
				try {
					Manager.file_ID = Integer.parseInt(cmds[1]);
				} catch (NumberFormatException e) {
					System.out.println("file_id must be integer.");
					return false;
				}
				Manager.cmd_type = Operation.fetch;
				Manager.save_path = cmds[2];
			}
		} else if (cmds[0].equals("recover")) {
			if (cmds.length != 2) {
				System.out.println("recover Usage: recover file_id");
				return false;
			} else {
				try {
					Manager.file_ID = Integer.parseInt(cmds[1]);
				} catch (NumberFormatException e) {
					System.out.println("file_id must be integer.");
					return false;
				}
				Manager.cmd_type = Operation.recover;
				return true;
			}
		} else {
			System.out.println("Command not found.Please use put|ls|fetch|read|recover|quit.");
			return false;
		}
		return true;
	}

	public static void main(String[] args) throws BrokenBarrierException, InterruptedException {
		Main main = new Main();
		main.init();
		main.start();
	}
}
