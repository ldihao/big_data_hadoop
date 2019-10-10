package Tools;

import java.io.Serializable;

public class MyFile implements Serializable{
    private static final long serialVersionUID = 1L;

    final private String name;

    final private String size;

    final private String[] blocks;

    public MyFile(String filename, String filesize ,String[] blk){
        this.name = filename;
        this.size = filesize;
        blocks = blk;
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public String[] getBlocks() {
        return blocks;
    }
}
