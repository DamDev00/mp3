import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class PlayReader{

    private String src;
    private java.util.Scanner scanner = new Scanner(System.in);
    private static Object lock = new Object();
    private int milliseconds = 0;
    private Clip current_clip;
    public static boolean thread_current_interrupted = false;
    private boolean avalaible = true;
    private final int HEIGHT = 400, WIDTH = 400;
    private final int MAX_CAPACITY = 5;
    private java.util.List<Thread> thread_queue = new ArrayList<>();
    private int priority_thread = 10;
    public Clip clip;
    private String current_directory = "C:\\Users\\Ottaviano\\Desktop\\Java visual studio\\Multithread\\app";
    private String DIRECTORY = "C:\\Users\\Ottaviano\\Desktop\\Java visual studio\\Multithread\\app";
    public static java.util.List<String> list_songs = new ArrayList<>();
    private boolean IsStop = false;
    private boolean restart = false;

    private class InformationPC{
        public static int HEIGHT = 1080, WIDTH = 1920;
        private  int[] resolution = {HEIGHT, WIDTH};

        public  int[] getResolution(){
            return resolution;
        }
    }

    public PlayReader(String file){
        this.current_directory += "\\".concat(file);
        try{
            setList(list_songs, current_directory);
        }catch (IOException e){
            System.out.println(e);
        }
    }

    private void setList(List<String> list, String directory)
            throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(
                "C:\\Users\\Ottaviano\\Desktop\\Java visual studio\\Multithread\\app\\songs.txt"
        ));
        String line = " ", tmp = " ";

        int reader = bufferedReader.read();

        while(reader != -1){
            char c = (char) reader;
            line+=c;
            reader = bufferedReader.read();
        }

        for(int i = 0; line.length() > i; i++){
            if(line.charAt(i) == ','){
                continue;
            } else {
                tmp += line.charAt(i);
            }
            if(i == line.length()-1){
                String tmp2 = " ";
                for(int j = 0; tmp.length() > j; j++){
                    if(tmp.charAt(j) == ' ' && tmp2 != " "
                            || j == tmp.length()-1  && tmp2 != " "){
                        if(j == tmp.length() - 1){
                            tmp2 += tmp.charAt(j);
                        }
                        list.add(tmp2.trim());
                        tmp2 = " ";
                    } else {
                        tmp2 += tmp.charAt(j);
                    }
                }
            }
        }
        for(int i = 0; list_songs.size() > i; i++){
            if(list_songs.get(i).isEmpty()){
                list_songs.remove(i);
            }
        }
    }

    private void setClip(Clip clip){
        this.current_clip = this.clip;
    }
    private Clip getClip(){
        return this.current_clip;
    }

    public synchronized void play(String name_song) throws InterruptedException,
            UnsupportedAudioFileException, IOException, LineUnavailableException {
        while(!avalaible){
            String name_song_current = name_song;
            if(thread_queue.size() <= this.MAX_CAPACITY){
                Thread.currentThread().setPriority((
                        this.priority_thread == 5 ? Thread.NORM_PRIORITY : this.priority_thread
                ));
                thread_queue.add(Thread.currentThread());
                System.out.println(Thread.currentThread().getName() + " inserito in coda");
                this.priority_thread--;
            } else {
                try{
                    System.out.println("impossibile inserire altri elementi in coda!");
                    Thread.currentThread().interrupt();
                }catch (Exception e){
                    System.err.println(e);
                }
            }
            wait();
        }
        avalaible = false;
        String name_song_current = name_song;
        System.out.println(name_song_current + " avviata");
        File file = null;
        try{
            file = new File(DIRECTORY.concat("\\" + name_song_current));
        }catch (Exception exception){
            System.err.println(exception);
        }
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        this.clip = AudioSystem.getClip();
        this.clip.open(audioInputStream);
        setClip(clip);
        clip.start();

        try{
            Thread task = new Thread(() -> {
                control();
            });
            task.start();
            wait();
        }catch (Exception e){
            System.err.println(e);
        }finally {
            avalaible = true;
            notify();
            System.out.println(name_song_current + " terminata");
        }
    }

    public synchronized void control(){
        while(this.clip.getMicrosecondLength() > this.clip.getMicrosecondPosition()){
            continue;
        }
        notify();
    }

    public long getMillisecondsPosition(){
        return this.clip.getMicrosecondPosition();
    }

    public void stop() throws InterruptedException {
        IsStop = true;
    }

    public int getLostTime(){
        return this.milliseconds;
    }

    private static <T extends Clip & Comparable<T>> long getTimeClip(Clip time){
        int ms = 1000;
        long microsecond = time.getMicrosecondLength();
        return (microsecond / ms) + 2000;
    }
    private static long getTimeClip(long time){
        return (time / 1000) + 2000;
    }

    public void counterMilliseconds(){
        while(!this.avalaible){
            while(PlayReader.thread_current_interrupted == true){
                this.milliseconds++;
            }
        }
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException,
            LineUnavailableException, InterruptedException {
        PlayReader playReader = new PlayReader("songs.txt");
        Scanner in = new Scanner(System.in);
        boolean counter_stop = false;

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playReader.play("song2.wav");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (UnsupportedAudioFileException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (LineUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "thread1");

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playReader.play("song.wav");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (UnsupportedAudioFileException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (LineUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        },"thread2");
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playReader.play("song2.wav");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (UnsupportedAudioFileException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (LineUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "thread3");
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                playReader.counterMilliseconds();
            }
        });

        t1.start();
        t2.start();
        t3.start();
        String line = "0";
        while(!line.equals("-1")){
            String l = in.next();
            if(l.equals("1")){
                playReader.clip.stop();
            }
            if(l.equals("2")){
                playReader.clip.start();
                PlayReader.thread_current_interrupted = false;
            }
            if(l.equals("3")){
                System.out.println(playReader.getMillisecondsPosition());
            }
        }
    }

}
