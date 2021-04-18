import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Test {

    public static LinkedBlockingQueue<Integer> buffer = new LinkedBlockingQueue<>(10);

    public static void main(String[] args) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Run");
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 5000, 5000);
        try {
            Thread.sleep(12500);
        } catch(Exception e){
            e.printStackTrace();
        }
        task.cancel();
    }
}