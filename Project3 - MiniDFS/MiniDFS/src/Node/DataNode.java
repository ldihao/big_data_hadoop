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
			if (Manager.cmd_type == Operation.recover) {
				if (Manager.needRecover.contains(id))
					recover();

				try {
					Manager.finishrecover_event.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				if (Manager.cmd_type == Operation.put && Manager.blockServer.containsKey(id)) {
					save();
				} else if (Manager.cmd_type == Operation.read)
					read();
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
	}

	public void save() {
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
		for (String blockName : Manager.recover_datanode.keySet()) {
			String srcPath = "dfs/datanode" + Manager.recover_datanode.get(blockName) + "/";
			for (int serverId : Manager.needRecover) {
				String desPath = "dfs/datanode" + serverId + "/";
				FileHelper.recover(srcPath, desPath, blockName);
			}
		}
	}

}
