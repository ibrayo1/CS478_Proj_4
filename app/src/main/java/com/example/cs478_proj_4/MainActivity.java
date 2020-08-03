package com.example.cs478_proj_4;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Integer> images = new ArrayList<>();

    // keeps track of all the position previously selected
    private ArrayList<Integer> selectedPos = new ArrayList<>();

    // stores all of the positions where there are near misses
    private ArrayList<Integer> nearMisses = new ArrayList<>();
    private ArrayList<Integer> closeGuesses = new ArrayList<>();

    private int gopherPosition;
    private GridAdapter gridAdapter;

    volatile boolean stopWorker = false;

    // values to be used by handleMessage()
    public static final int SUCCESS = 0;
    public static final int NEAR_MISS = 1;
    public static final int CLOSE_GUESS = 2;
    public static final int COMPLETE_MISS = 3;
    public static final int DISASTER = 4;

    // this 2d matrix is used to find the adjacent positions for near and close misses/guesses
    int[][] matrix = {{ 0,  1,  2,  3,  4,  5,  6,  7,  8,  9},
                      {10, 11, 12, 13, 14, 15, 16, 17, 18, 19},
                      {20, 21, 22, 23, 24, 25, 26, 27, 28, 29},
                      {30, 31, 32, 33, 34, 35, 36, 37, 38, 39},
                      {40, 41, 42, 43, 44, 45, 46, 47, 48, 49},
                      {50, 51, 52, 53, 54, 55, 56, 57, 58, 59},
                      {60, 61, 62, 63, 64, 65, 66, 67, 68, 69},
                      {70, 71, 72, 73, 74, 75, 76, 77, 78, 79},
                      {80, 81, 82, 83, 84, 85, 86, 87, 88, 89},
                      {90, 91, 92, 93, 94, 95, 96, 97, 98, 99}};

    private WorkerThread1 workerthread1 = new WorkerThread1();
    private WorkerThread2 workerthread2 = new WorkerThread2();
    TextView thread_1_text;
    TextView thread_2_text;

    // keeps track of near misses for
    private ArrayList<Integer> thread1NearMisses = new ArrayList<>();
    private volatile boolean threadNearMiss = false;
    private int counter = 0;
    private int thread1guess = 100;

    // message handler for worker thread 1
    private Handler mHandler1 = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case SUCCESS:
                    thread_1_text.append("success");
                    break;
                case NEAR_MISS:
                    thread_1_text.append("near miss");
                    break;
                case CLOSE_GUESS:
                    thread_1_text.append("close guess");
                    break;
                case DISASTER:
                    thread_1_text.append("disaster");
                    break;
                case COMPLETE_MISS:
                    thread_1_text.append("complete miss");
                    break;
                default:
                    break;
            }

        }
    };

    // message handler for worker thread 2
    private Handler mHandler2 = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case SUCCESS:
                    thread_2_text.append("success");
                    break;
                case NEAR_MISS:
                    thread_2_text.append("near miss");
                    break;
                case CLOSE_GUESS:
                    thread_2_text.append("close guess");
                    break;
                case DISASTER:
                    thread_2_text.append("disaster");
                    break;
                case COMPLETE_MISS:
                    thread_2_text.append("complete miss");
                    break;
                default:
                    break;
            }

        }

    };

    // create the handler for the ui and random number generator
    Handler handler = new Handler();
    Random random = new Random();

    // declare all our buttons
    Button restart;
    Button guess_btn;
    Button cont_btn;
    Button play_t1;
    Button play_t2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // add the images to the images array list
        setImages(images);

        // set the 10x10 grid with hole images
        GridView gridView = (GridView) findViewById(R.id.grid);
        gridAdapter = new GridAdapter(this, images);
        gridView.setAdapter(gridAdapter);

        // randomly place the gopher on the board
        gopherPosition = random.nextInt(100);
        gridAdapter.setGopherPosition(gopherPosition);
        gridAdapter.notifyDataSetChanged();

        // find the positions of the near and close misses/guesses
        findNearAndCloseMiss(gopherPosition);

        // declare message board for each thread
        thread_1_text = (TextView) findViewById(R.id.thread_1_text);
        thread_2_text = (TextView) findViewById(R.id.thread_2_text);

        // start the threads
        workerthread1.start();
        workerthread2.start();

        // continuous button activates the threads to continuous mode
        cont_btn = (Button) findViewById(R.id.continuous_mode);
        cont_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWorker = false;
                continuous_workerthread_one();
                continuous_workerthread_two();
                cont_btn.setVisibility(View.GONE);
                play_t1.setVisibility(View.GONE);
                //play_t2.setVisibility(View.GONE);
                guess_btn.setVisibility(View.VISIBLE);
            }
        });

        // this button activates the guess-by-guess mode
        guess_btn = (Button) findViewById(R.id.guess_mode);
        guess_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWorker = true;
                guess_btn.setVisibility(View.GONE);
                cont_btn.setVisibility(View.VISIBLE);
                play_t1.setVisibility(View.VISIBLE);
                //play_t2.setVisibility(View.VISIBLE);
            }
        });

        // after a thread wins player is given option to restart the game
        restart = (Button) findViewById(R.id.play_again_btn);
        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        play_t1 = (Button) findViewById(R.id.thread_1_btn);
        play_t1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thread1_guess();
            }
        });

//        play_t2 = (Button) findViewById(R.id.thread_2_btn);
//        play_t2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                thread2_guess();
//            }
//        });

    }


    public void setImages(ArrayList<Integer> imgs){
        for(int i = 0; i < 100; i++){
            imgs.add(R.drawable.hole);
        }
    }

    public void findNearAndCloseMiss(int pos){
        int x = 0; int y = 0;

        // find the index from the position choosen
        for(int i = 0; i < 10; i++){
            for(int j = 0; j < 10; j++){
                if(pos == matrix[i][j]){ x = i; y = j; }
            }
        }

        // find all of the adjacent positions for near miss
        for(int i = x-1; i <= x+1; i++){
            for(int  j = y-1; j <= y+1; j++){
                if(i >= 0 && i <= 9 && j >=0 && j <= 9){
                    nearMisses.add(matrix[i][j]);
                }
            }
        }

        // find all of the adjacent positions for close guess
        for(int i = x-2; i <= x+2; i++){
            for(int  j = y-2; j <= y+2; j++){
                if(i >= 0 && i <= 9 && j >=0 && j <= 9){
                    closeGuesses.add(matrix[i][j]);
                }
            }
        }

        // remove all of the near guess positions from close guesses array
        closeGuesses.removeAll(nearMisses);

        // above loop also contains position of gopher in near misses array so we remove it
        int indexOfGopher = nearMisses.indexOf(gopherPosition);
        nearMisses.remove(indexOfGopher);

        // log the data
        //for(int i = 0; i < nearMisses.size(); i++){
        //    Log.i("Array", nearMisses.get(i).toString());
        //}

        //for(int i = 0; i < closeGuesses.size(); i++){
        //    Log.i("Array2", closeGuesses.get(i).toString());
        //}

    }

    public void continuous_workerthread_one(){
        workerthread1.handler.post(new Runnable() {
            @Override
            public void run() {
                while(!stopWorker){

                    int bruh = getThread1guess();

                    // if the guess is a near miss then
                    if(nearMisses.contains(bruh)){
                        getThread1NearMisses(bruh);
                        threadNearMiss = true;
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            int bro;
                            if(threadNearMiss) {
                                bro = thread1NearMisses.get(counter);
                                counter++;
                            }
                            else
                                bro = bruh;

                            gridAdapter.setSelectedPosition(bro, 255, 0, 0);
                            gridAdapter.notifyDataSetChanged();

                            if(bro == gopherPosition) {
                                mHandler1.sendMessage(mHandler1.obtainMessage(SUCCESS));
                                thread_1_text.append("\n" + bro + " - ");
                                Toast.makeText(getBaseContext(), "Thread 1 Won", Toast.LENGTH_SHORT).show();
                                guess_btn.setVisibility(View.GONE);
                                cont_btn.setVisibility(View.GONE);
                                play_t1.setVisibility(View.GONE);
                                //play_t2.setVisibility(View.GONE);
                                restart.setVisibility(View.VISIBLE);
                            } else if (selectedPos.contains(bro)){
                                mHandler1.sendMessage(mHandler1.obtainMessage(DISASTER));
                                thread_1_text.append("\n" + bro + " - ");
                            } else if (nearMisses.contains(bro)){
                                mHandler1.sendMessage(mHandler1.obtainMessage(NEAR_MISS));
                                thread_1_text.append("\n" + bro + " - ");
                            } else if (closeGuesses.contains(bro)){
                                mHandler1.sendMessage(mHandler1.obtainMessage(CLOSE_GUESS));
                                thread_1_text.append("\n" + bro + " - ");
                            } else {
                                mHandler1.sendMessage(mHandler1.obtainMessage(COMPLETE_MISS));
                                thread_1_text.append("\n" + bro + " - ");
                            }

                            if(!selectedPos.contains(bro))
                                selectedPos.add(bro);

                            if(bro == gopherPosition){
                                stopWorker = true;
                            }
                        }
                    });

//                    if(bruh == gopherPosition){
//                        stopWorker = true;
//                    }

                    SystemClock.sleep(1000);
                }
            }
        });
    }

    public void continuous_workerthread_two(){
        workerthread2.handler.post(new Runnable() {
            @Override
            public void run() {
                while(!stopWorker){

                    int bruh = random.nextInt(100);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            gridAdapter.setSelectedPosition(bruh, 0, 0, 255);
                            gridAdapter.notifyDataSetChanged();

                            if(bruh == gopherPosition) {
                                mHandler2.sendMessage(mHandler2.obtainMessage(SUCCESS));
                                thread_2_text.append("\n" + bruh + " - ");
                                Toast.makeText(getBaseContext(), "Thread 2 Won", Toast.LENGTH_SHORT).show();
                                guess_btn.setVisibility(View.GONE);
                                cont_btn.setVisibility(View.GONE);
                                play_t1.setVisibility(View.GONE);
                                //play_t2.setVisibility(View.GONE);
                                restart.setVisibility(View.VISIBLE);
                            } else if (selectedPos.contains(bruh)){
                                mHandler2.sendMessage(mHandler2.obtainMessage(DISASTER));
                                thread_2_text.append("\n" + bruh + " - ");
                            } else if (nearMisses.contains(bruh)){
                                mHandler2.sendMessage(mHandler2.obtainMessage(NEAR_MISS));
                                thread_2_text.append("\n" + bruh + " - ");
                            } else if (closeGuesses.contains(bruh)){
                                mHandler2.sendMessage(mHandler2.obtainMessage(CLOSE_GUESS));
                                thread_2_text.append("\n" + bruh + " - ");
                            } else {
                                mHandler2.sendMessage(mHandler2.obtainMessage(COMPLETE_MISS));
                                thread_2_text.append("\n" + bruh + " - ");
                            }

                            if(!selectedPos.contains(bruh))
                                selectedPos.add(bruh);
                        }
                    });

                    if(bruh == gopherPosition){
                        stopWorker = true;
                    }

                    SystemClock.sleep(1005);
                }
            }
        });
    }

    public void thread1_guess(){
        workerthread1.handler.post(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(100);

                int bruh = getThread1guess();

                // if the guess is a near miss then
                if(nearMisses.contains(bruh)){
                    getThread1NearMisses(bruh);
                    threadNearMiss = true;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        int bro;
                        if(threadNearMiss) {
                            bro = thread1NearMisses.get(counter);
                            counter++;
                        }
                        else
                            bro = bruh;

                        gridAdapter.setSelectedPosition(bro, 255, 0, 0);
                        gridAdapter.notifyDataSetChanged();

                        if(bro == gopherPosition) {
                            mHandler1.sendMessage(mHandler1.obtainMessage(SUCCESS));
                            thread_1_text.append("\n" + bro + " - ");
                            Toast.makeText(getBaseContext(), "Thread 1 Won", Toast.LENGTH_SHORT).show();
                            guess_btn.setVisibility(View.GONE);
                            cont_btn.setVisibility(View.GONE);
                            play_t1.setVisibility(View.GONE);
                            //play_t2.setVisibility(View.GONE);
                            restart.setVisibility(View.VISIBLE);
                        } else if (selectedPos.contains(bro)){
                            mHandler1.sendMessage(mHandler1.obtainMessage(DISASTER));
                            thread_1_text.append("\n" + bro + " - ");
                        } else if (nearMisses.contains(bro)){
                            mHandler1.sendMessage(mHandler1.obtainMessage(NEAR_MISS));
                            thread_1_text.append("\n" + bro + " - ");
                        } else if (closeGuesses.contains(bro)){
                            mHandler1.sendMessage(mHandler1.obtainMessage(CLOSE_GUESS));
                            thread_1_text.append("\n" + bro + " - ");
                        } else {
                            mHandler1.sendMessage(mHandler1.obtainMessage(COMPLETE_MISS));
                            thread_1_text.append("\n" + bro + " - ");
                        }

                    }
                });
            }
        });

        workerthread2.handler.post(new Runnable() {
            @Override
            public void run() {
                int bruh = random.nextInt(100);

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        gridAdapter.setSelectedPosition(bruh, 0, 0, 255);
                        gridAdapter.notifyDataSetChanged();

                        if(bruh == gopherPosition) {
                            mHandler2.sendMessage(mHandler2.obtainMessage(SUCCESS));
                            thread_2_text.append("\n" + bruh + " - ");
                            Toast.makeText(getBaseContext(), "Thread 2 Won", Toast.LENGTH_SHORT).show();
                            guess_btn.setVisibility(View.GONE);
                            cont_btn.setVisibility(View.GONE);
                            play_t1.setVisibility(View.GONE);
                            //play_t2.setVisibility(View.GONE);
                            restart.setVisibility(View.VISIBLE);
                        } else if (selectedPos.contains(bruh)){
                            mHandler2.sendMessage(mHandler2.obtainMessage(DISASTER));
                            thread_2_text.append("\n" + bruh + " - ");
                        } else if (nearMisses.contains(bruh)){
                            mHandler2.sendMessage(mHandler2.obtainMessage(NEAR_MISS));
                            thread_2_text.append("\n" + bruh + " - ");
                        } else if (closeGuesses.contains(bruh)){
                            mHandler2.sendMessage(mHandler2.obtainMessage(CLOSE_GUESS));
                            thread_2_text.append("\n" + bruh + " - ");
                        } else {
                            mHandler2.sendMessage(mHandler2.obtainMessage(COMPLETE_MISS));
                            thread_2_text.append("\n" + bruh + " - ");
                        }

                        if(!selectedPos.contains(bruh))
                            selectedPos.add(bruh);
                    }
                });

            }
        });
    }


//    public void thread2_guess(){
//        workerthread2.handler.post(new Runnable() {
//            @Override
//            public void run() {
//                int bruh = random.nextInt(100);
//
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        gridAdapter.setSelectedPosition(bruh, 0, 0, 255);
//                        gridAdapter.notifyDataSetChanged();
//
//                        if(bruh == gopherPosition) {
//                            mHandler2.sendMessage(mHandler2.obtainMessage(SUCCESS));
//                            thread_2_text.append("\n" + bruh + " - ");
//                            Toast.makeText(getBaseContext(), "Thread 2 Won", Toast.LENGTH_SHORT).show();
//                            guess_btn.setVisibility(View.GONE);
//                            cont_btn.setVisibility(View.GONE);
//                            play_t1.setVisibility(View.GONE);
//                            play_t2.setVisibility(View.GONE);
//                            restart.setVisibility(View.VISIBLE);
//                        } else if (selectedPos.contains(bruh)){
//                            mHandler2.sendMessage(mHandler2.obtainMessage(DISASTER));
//                            thread_2_text.append("\n" + bruh + " - ");
//                        } else if (nearMisses.contains(bruh)){
//                            mHandler2.sendMessage(mHandler2.obtainMessage(NEAR_MISS));
//                            thread_2_text.append("\n" + bruh + " - ");
//                        } else if (closeGuesses.contains(bruh)){
//                            mHandler2.sendMessage(mHandler2.obtainMessage(CLOSE_GUESS));
//                            thread_2_text.append("\n" + bruh + " - ");
//                        } else {
//                            mHandler2.sendMessage(mHandler2.obtainMessage(COMPLETE_MISS));
//                            thread_2_text.append("\n" + bruh + " - ");
//                        }
//
//                        if(!selectedPos.contains(bruh))
//                            selectedPos.add(bruh);
//                    }
//                });
//
//            }
//        });
//    }

    public int getThread1guess(){
        return --thread1guess;
    }

    public void getThread1NearMisses(int pos){
        int x = 0; int y = 0;


        // find the index from the position choosen
        for(int i = 0; i < 10; i++){
            for(int j = 0; j < 10; j++){
                if(pos == matrix[i][j]){ x = i; y = j; }
            }
        }

        // find all of the adjacent positions for near miss
        for(int i = x-2; i <= x+2; i++){
            for(int  j = y-2; j <= y+2; j++){
                if(i >= 0 && i <= 9 && j >=0 && j <= 9){
                    thread1NearMisses.add(matrix[i][j]);
                }
            }
        }
    }
}
