import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Test {

    public static LinkedBlockingQueue<Integer> buffer = new LinkedBlockingQueue<>(10);

    public static void main(String[] args) {
        Timeout timeout = new Timeout(5_000_000_000L);
        for(int i = 0; i < 10; i++) {
            timeout.update(i, System.nanoTime());
        }
        System.out.println(timeout.getTo());
        System.out.println(timeout.getErtt());
        System.out.println(timeout.getEdev());
    }
}