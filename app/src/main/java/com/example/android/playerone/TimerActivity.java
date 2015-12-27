package com.example.android.playerone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TimerActivity extends ActionBarActivity {

    final static String DEBUG_TAG = "DEBUG_TIMER";

    private int[] mColors;
    private TextView mTimerView;
    private DrawingView mDrawingView;
    private Handler mFinishHandler;
    HashMap<Integer, PlayerFinger> mPlayers;
    // TODO: in theory, this could be a valid key. We need to replace it with something impossible.
    private Integer mWinningId = -1;
    private boolean mWinnerChosen = false;
    private int mNumColorsUsed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        mColors = getResources().getIntArray(R.array.androidcolors);
        mTimerView = (TextView)findViewById(R.id.timer_text_view);
        mPlayers = new HashMap<>();

        FrameLayout layout = (FrameLayout)findViewById(R.id.timer_layout);
        layout.removeView(mTimerView);
        mDrawingView = new DrawingView(TimerActivity.this);
        layout.addView(mDrawingView);
        layout.addView(mTimerView);

        mFinishHandler = new Handler();

        // show a 10 second timer that updates every second
        new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {
                mTimerView.setText("" + millisUntilFinished / 1000);
            }

            // Stop this activity 5 seconds after the timer runs out.
            public void onFinish() {
                if (mPlayers.size() > 0) {
                    mTimerView.setText("Winner!");
                    chooseWinner(mPlayers);
                    // Refresh the drawings for the players.
                    mFinishHandler.post(new Runnable() {
                        public void run() {
                            mDrawingView.drawPlayers(mPlayers);
                        }
                    });
                } else {
                    mTimerView.setText("");
                }
                // End the activity after a short delay.
                mFinishHandler.postDelayed(new Runnable() {
                    public void run() {
                        TimerActivity.this.finish();
                    }
                }, 5000);
            }
        }.start();
    }

    /**
     * Choose a random player.
     *
     * @param players
     */
    private void chooseWinner(HashMap<Integer, PlayerFinger> players) {

        List<Integer> keys = new ArrayList<Integer>(players.keySet());

        if (keys.size() > 0) {
            Random rand = new Random();
            int winningIdx = rand.nextInt(players.size());
            mWinningId = keys.get(rand.nextInt(keys.size()));
        }

        mWinnerChosen = true;
    }

    /**
     * Used for drawing circles at all the touched locations.
     */
    class DrawingView extends SurfaceView {
        private final SurfaceHolder surfaceHolder;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public DrawingView(Context context) {
            super(context);
            surfaceHolder = getHolder();
            paint.setStyle(Paint.Style.FILL);
        }

        /**
         * @param event The MotionEvent that just happened.
         * @return Returns true if the event was handled.
         */
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);
            boolean returnVal;
            switch (action) {
                // When last finger is up, I want to paint it black.
                case (MotionEvent.ACTION_UP):
                case (MotionEvent.ACTION_POINTER_UP):
                    removePlayer(mPlayers, event);
                    returnVal = true;
                    break;
                case (MotionEvent.ACTION_DOWN):
                case (MotionEvent.ACTION_POINTER_DOWN):
                    addPlayer(mPlayers, event);
                    returnVal = true;
                    break;
                case (MotionEvent.ACTION_MOVE):
                    updatePlayers(mPlayers, event);
                    returnVal = true;
                    break;
                default:
                    returnVal = super.onTouchEvent(event);
            }

            drawPlayers(mPlayers);
            return returnVal;
        }

        /**
         * Remove the player in the event from the HashMap.
         *
         * @param players
         * @param event
         */
        private void removePlayer(HashMap<Integer, PlayerFinger> players, MotionEvent event) {
            Integer id = event.getPointerId(event.getActionIndex());
            players.remove(id);
        }

        /**
         * Add a new PlayerFingers to the HashMap if they isn't already there.
         *
         * @param players
         * @param event
         */
        private void addPlayer(HashMap<Integer, PlayerFinger> players, MotionEvent event) {
            int index = event.getActionIndex();
            Integer id = event.getPointerId(index);
            if (!players.containsKey(id)) {
                players.put(id, new PlayerFinger(event.getX(index), event.getY(index)));
            }
        }

        /**
         * Update the players position in this event.
         *
         * @param players
         * @param event
         */
        private void updatePlayers(HashMap<Integer, PlayerFinger> players, MotionEvent event) {
            for (int index = 0; index < event.getPointerCount(); index++) {
                Integer id = event.getPointerId(index);
                // Just in case we get a MOTION_MOVE before a MOTION_DOWN
                if (!players.containsKey(id)) {
                    players.put(id, new PlayerFinger(event.getX(index), event.getY(index)));
                } else {
                    PlayerFinger player = players.get(id);
                    player.mXpos = event.getX(index);
                    player.mYpos = event.getY(index);
                }
            }
        }

        public void drawPlayers(HashMap<Integer, PlayerFinger> players) {
            if (surfaceHolder.getSurface().isValid()) {
                Canvas canvas = surfaceHolder.lockCanvas();
                canvas.drawColor(Color.BLACK);
                for (Integer playerId : mPlayers.keySet()) {
                    PlayerFinger player = mPlayers.get(playerId);
                    if (mWinnerChosen) {
                        // If we have already chosen a winner, only show the winner.
                        if (playerId == mWinningId) {
                            paint.setColor(Color.WHITE);
                            canvas.drawCircle(player.mXpos, player.mYpos, 85, paint);
                            paint.setColor(player.getmColor());
                            canvas.drawCircle(player.mXpos, player.mYpos, 75, paint);
                        }
                    } else {
                        // We haven't chosen a winner, so display everyone.
                        paint.setColor(player.getmColor());
                        canvas.drawCircle(player.mXpos, player.mYpos, 75, paint);
                    }
                }
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Holds information about a player's finger on the screen.
     */
    private class PlayerFinger {
        public float mXpos;
        public float mYpos;
        private final int mColor;

        public PlayerFinger(float xPos, float yPos) {
            mXpos = xPos;
            mYpos = yPos;
            mColor = mColors[mNumColorsUsed % mColors.length];
            mNumColorsUsed++;
        }

        public int getmColor() {
            return mColor;
        }
    }
}

