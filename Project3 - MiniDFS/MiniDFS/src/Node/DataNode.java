package Node;

import java.util.concurrent.BrokenBarrierException;

import Main.Manager;
import Tools.Block;
import Tools.FileHelper;
import Tools.Operation;

public class DataNode extends Thread {

	private int id;

	public DataNode(int serverId) {
		super();
		this.id = serverId;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Manager.data_event[id].await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}
			if (Manager.cmd_type == Operation.put && Manager.blockServer.containsKey(id)) {
				save();
			} else if (Manager.cmd_type == Operation.read)
				read();
			else if (Manager.cmd_type == Operation.recover && Manager.blockServer.containsKey(id))
				recover();
			else {
				try {
					Manager.main_event[id].await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
			}

		}
	}

	public void save() {
		//System.out.println("Size of server " + id + ": " + Manager.blockServer.get(id).size());
		for (Block block : Manager.blockServer.get(id)) {
			FileHelper.write("dfs/datanode" + id + "/" + block.getName(), block.getOffset());
		}
		
		Manager.blockServer.clear();
		
		try {
			Manager.main_event[id].await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}

	}

	public void read() {
		String path = "dfs/datanode" + id + "/";
		String filename = Manager.file_ID + "-part-0";
		FileHelper.read(path + filename);
		try {
			Manager.read_event.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	public void recover() {
		// todo
		String path = "dfs/datanode" + id + "/";
		String filename = Manager.file_ID + "-part-0";
		FileHelper.recover();
		try {
			Manager.read_event.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

}
