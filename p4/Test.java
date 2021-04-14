import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Test {

    public static LinkedBlockingQueue<Integer> buffer = new LinkedBlockingQueue<>(10);

    public static void main(String[] args) {
        try {
            for (int i = 0; i < 10; i++) {
                buffer.put(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(buffer);
        new Thread(()->producer()).start();
        new Thread(()->consumer()).start();
    }

    public static void producer() {
        try {
            for (int i = 0; i < 10; i++) {
                buffer.put(i * 10);
            }
            System.out.println(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void consumer() {
        for (Integer i : buffer) {
            System.out.println(i);
        }
        buffer.clear();
    }
}