package Tools;

import java.io.*;

import Main.Manager;

public class FileHelper {

    public static void read(String filepath){
        File file = new File(filepath);
        try ( FileInputStream fis = new FileInputStream(file);){
            int size;
            byte[] b = new byte[1024];
            if((size = fis.read(b))!=-1)
                System.out.println(new String(b));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(String des,long offset){
        File src_file = new File(Manager.file_path);
        File des_file = new File(des);
        int size;
        byte[] b = new byte[Manager.BLOCK_SIZE];
        try ( FileInputStream fis = new FileInputStream(src_file);
              FileOutputStream fos = new FileOutputStream(des_file);){

            fis.skip(offset);
            if((size=fis.read(b))!=-1)
                fos.write(b,0,size);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void recover() {
    	//todo
    	
    }

}
