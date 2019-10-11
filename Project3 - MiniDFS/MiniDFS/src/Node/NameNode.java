package Node;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;

import Main.Manager;
import Tools.Block;
import Tools.FileMap;
import Tools.MyFile;

public class NameNode extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;

	private FileMap fileMap;
	private Random ran = new Random();

	public void init() {
		if (!new File("dfs/namenode/map").exists()) {
			fileMap = new FileMap();
		} else
			load();

	}

	@Override
	public void run() {
		while (true) {
			try {
				Manager.name_event.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}
			switch (Manager.cmd_type) {
			case ls:
				listFile();
				break;
			case put:
				splitFile();
				break;
			case read:
				readFile();
				break;
			case fetch:
				fetchFile();
				break;
			case recover:
				recoverFile();
				break;
			}
		}
	}

	@Override
	public void start() {
		init();
		super.start();
	}

	public void updateData() {
		try {
			FileOutputStream fos = new FileOutputStream("dfs/namenode/map");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(fileMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void load() {
		try {
			FileInputStream fis = new FileInputStream("dfs/namenode/map");
			ObjectInputStream ois = new ObjectInputStream(fis);
			fileMap = (FileMap) ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void listFile() {
		System.out.printf("%-10s%-20s%-10s%n", "ID", "file_name", "file_size");
		Vector<MyFile> vec = fileMap.id_file;
		for (int i = 0; i < vec.size(); i++) {
			System.out.printf(Manager.OUTPUT_FORMAT, i, vec.get(i).getName(), vec.get(i).getSize());
		}
		try {
			Manager.ls_event.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	public void splitFile() {
		File upfile = new File(Manager.file_path);
		long file_size = upfile.length();
		int file_id = fileMap.lastId;
		int block_counts = (int) (file_size / Manager.BLOCK_SIZE) + 1;
		String[] blk = new String[block_counts];
		for (int i = 0; i < block_counts; i++) {
			// find the server that doesn't store replicas
			int not_server = ran.nextInt(4);

			for (int j = 0; j < 4; j++) {
				if (j != not_server) {
					int assign_server = j;

					String blkName = file_id + "-part-" + i;
					blk[i] = blkName;
					Block block = new Block(blkName, Manager.BLOCK_SIZE * i);

					if (Manager.blockServer.containsKey(assign_server)) {
						Manager.blockServer.get(assign_server).add(block);
					} else {
						ArrayList<Block> blocks = new ArrayList<>();
						blocks.add(block);
						Manager.blockServer.put(assign_server, blocks);
					}

					if (fileMap.block_datanode.containsKey(blkName)) {
						fileMap.block_datanode.get(blkName).add(assign_server);
					} else {
						ArrayList<Integer> servers = new ArrayList<>();
						servers.add(assign_server);
						fileMap.block_datanode.put(blkName, servers);
					}
				}
			}
		}
		for (int m = 0; m < 4; m++) {
			try {
				Manager.data_event[m].await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
		MyFile myFile = new MyFile(Manager.file_path, new Long(file_size).toString(), blk);
		fileMap.id_file.add(myFile);
		fileMap.lastId++;
		fileMap.lastServer++;
		updateData();

	}

	public int getFirstAvailableBlock(String read_first_blk) throws Exception{
    	int i = 0;
        int serverId = fileMap.block_datanode.get(read_first_blk).get(i);
//        System.out.println(read_first_blk);
        String path = "dfs/datanode"+serverId+"/";
        String filename = read_first_blk;
//        System.out.println(filename);
        File tmp_file = new File(path+filename);
        while(!tmp_file.exists()) {
        	i++;
        	if(i==3) {
	        	throw new Exception("No available block! Please try to recovery this file.");
	        }
        	serverId = fileMap.block_datanode.get(read_first_blk).get(i);
//            System.out.println(serverId);
	        path = "dfs/datanode"+serverId+"/";
//	        filename = Manager.file_ID+"-part-0";
	        tmp_file = new File(path+filename);
        }
        return i;
    }

    public void readFile(){
        MyFile myFile = fileMap.id_file.get(Manager.file_ID);
        String read_first_blk = myFile.getBlocks()[0];
        int serverId=0;
        try
    	{
    		serverId = fileMap.block_datanode.get(read_first_blk).get(getFirstAvailableBlock(read_first_blk));
//    		Global.data_event[serverId].await();
    		Manager.data_event[serverId].await();
    	} catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } catch (Exception e) {
//    		e.printStackTrace();
        	System.out.println("Error! Cannot read this file.");
    		try {
				Manager.read_event.await();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (BrokenBarrierException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    	} 
//        try {
//        	Manager.read_event.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (BrokenBarrierException e) {
//            e.printStackTrace();
//        }
    }

    public void fetchFile(){
        MyFile myFile = fileMap.id_file.get(Manager.file_ID);
        File output = new File(Manager.save_path+"/"+myFile.getName());
        int serverId;
        byte[] b = new byte[Manager.BLOCK_SIZE];
        int size;
        try (FileOutputStream fos = new FileOutputStream(output);){
            for(String blk : myFile.getBlocks()){
//            	try
//            	{
            	serverId = fileMap.block_datanode.get(blk).get(getFirstAvailableBlock(blk));
//            	} catch (Exception e) {
//            		e.printStackTrace();
//            	}
                FileInputStream fis = new FileInputStream(new File("dfs/datanode"+serverId+"/"+blk));
                if((size = fis.read(b))!=-1)
                    fos.write(b,0,size);
                fis.close();
            }
            System.out.println("Fetch success!");
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } catch (Exception e) {
//    		e.printStackTrace();
        	System.out.println("Error! Cannot fetch this file.");
    	} finally {
    		try {
				Manager.main_event[0].await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
        
    }

	private boolean hasFile(String fileName) {
		File f = new File(fileName);
		if (f.exists() && !f.isDirectory())
			return true;
		else
			return false;
	}

	public void recoverFile() {
		MyFile myFile = fileMap.id_file.get(Manager.file_ID);
		String[] blockNames = myFile.getBlocks();
		
		boolean canRecover = true;

		for (int i = 0; i < blockNames.length; ++i) {
			List<Integer> servers = fileMap.block_datanode.get(blockNames[i]);
			int firstServer = -1;
			List<Integer> needRecover = Manager.needRecover;
			needRecover.clear();
			Manager.recover_datanode.clear();

			for (int j = 0; j < servers.size(); ++j) {
				int id = servers.get(j);
				String filePath = "dfs/datanode" + id + "/" + blockNames[i];
				boolean flag = hasFile(filePath);
				if (flag) {
					if (firstServer == -1)
						firstServer = id;
				} else {
					needRecover.add(id);
				}
			}
			if (needRecover.isEmpty())
				continue;
			else if (firstServer != -1) {
				Manager.recover_datanode.put(blockNames[i], firstServer);
				// recover the node
				for (int j = 0; j < Manager.SERVER_NUMBER; ++j) {
					try {
						Manager.data_event[j].await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
				}
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
				canRecover = false;
				break;
			}

		}
		if (!canRecover) {
			System.out.println("Sorry, file cannot be recovered.");
		} else
			System.out.println("Recover success!");

		for (int j = 0; j < Manager.SERVER_NUMBER; j++)
			try {
				Manager.main_event[j].await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
