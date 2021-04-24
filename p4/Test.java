import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

public class Test {

    public static void main(String[] args) {
        int i1 = Integer.parseInt​("10000110010111101010110001100000", 2);
        int i2 = Integer.parseInt​("01110001001010101000000110110101", 2);
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putInt(i1);
        bb.putInt(i2);
        
    }
}