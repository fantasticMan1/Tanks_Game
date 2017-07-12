package me.joshwinebrener.tanks;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //initialize (most) global variables
    MotionEvent tempEvent;
    View tempView;

    FrameLayout frame;
    ImageView dirt;
    ImageView bullet;
    ImageView[] flames = new ImageView[5];
    Tank tank;

    //for timer
    private Timer timer = new Timer();
    private Handler handler = new Handler();

    double degToTapTemp = 0.0;
    double radToTapTemp = 0.0;
    double angleDelta = 0.0;

    float tankX = 0.0f;
    float tankY = 0.0f;
    float tankRotation = 0.0f;
    float tapX = 0.0f;
    float tapY = 0.0f;

    boolean start_flg = false;
    boolean touching = false;

    boolean shoot = true;
    float bulletDirectionDeg = 0.0f;
    float bulletDirectionRad = 0.0f;
    float bulletImpactX = 0.0f;
    float bulletImpactY = 0.0f;
    int flameIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set values for all the views and frames
        frame = (FrameLayout) findViewById(R.id.frame);
        dirt = (ImageView) findViewById(R.id.dirt);
        bullet = (ImageView) findViewById(R.id.bullet);
        for(int i = 0; i < flames.length; i++) {
            flames[i] = new ImageView(this);
            flames[i].setImageResource(R.drawable.flame_icon);
            //set the dimensions of each flame
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40 ,40);
            flames[i].setLayoutParams(params);
            //hide the flames for now
            flames[i].setVisibility(View.INVISIBLE);
            frame.addView(flames[i]);
        }
        tank = new Tank();

        dirt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //create global variables containing view and event info
                tempEvent = event;
                tempView = v;
                //start_flg => only run this "if" statement once
                if (start_flg == false) {
                    start_flg = true;
                    //set a timer that will update the position every 20 millisecs
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    updatePos();
                                }
                            });
                        }
                    }, 0, 20);

                    //let the rest of the world know that the screen is being touched
                    touching = true;

                } else {
                    //let the rest of the world know that the screen is being touched
                    if (event.getAction() == MotionEvent.ACTION_DOWN) touching = true;
                    //let the rest of the world know that the screen is not being touched
                    else if (event.getAction() == MotionEvent.ACTION_UP) touching = false;
                }
                return true;
            }
        });

    }

    public void updatePos() {
        if(touching) {
            //calculate tapX and tapY relative to the dirt view
            int[] location = new int[2];
            tempView.getLocationOnScreen(location);
            float screenX = tempEvent.getRawX();
            float screenY = tempEvent.getRawY();
            tapX = screenX - location[0];
            tapY = screenY - location[1];

            // save tankX, tankY and tankRotation in temporary variables
            tankX = tank.getX();
            tankY = tank.getY();
            tankRotation = tank.getRotation();

            // save degToTapTemp to a variable to simplify
            degToTapTemp = degToTap(tapX, tapY, tankX, tankY);
            radToTapTemp = degToTapTemp * Math.PI / 180;

            //calculate the discrepancy between the rotation and the tap angle.
            //positive is right, negative is left
            angleDelta = degToTapTemp - tank.getRotation();
            if (angleDelta > 180) {
                angleDelta -= 360;
            } else if (angleDelta < -180) {
                angleDelta += 360;
            }

            //slowly correct the angle discrepancy between the tank and tap
            if (angleDelta > 1) {
                //increment the rotation
                tankRotation += 2;
            }
            if (angleDelta < -1) {
                // decrement the rotation
                tankRotation -= 2;
            }

            //slowly correct the distance discrepancy between the tank and the tap
            if (tank.getX() != tapX) {
                //increment or decrement tankX in the direction it is facing
                tankX -= 2 * (float) Math.cos(tank.getRotation() * Math.PI / 180);
            }
            if (tank.getY() != tapY) {
                //increment or decrement tankY in the direction it is facing
                tankY -= 2 * (float) Math.sin(tank.getRotation() * Math.PI / 180);
            }

            //shoot when pointing at the tap
            if (angleDelta < 1 && angleDelta > -1) {
                tank.shoot();
            }
        }

        //set the tank to the calculated coordinates and heading
        tank.setX(tankX);
        tank.setY(tankY);
        tank.setRotation(tankRotation);
    }

    public double degToTap(double tapX, double tapY, double tankX, double tankY) {
        //calculate the angle in radians using the inverse tangent, and then convert to degrees
        double degToTap = Math.atan((tapY - tankY) / (tapX - tankX)) * 180 / Math.PI;

        //screen fourth quadrant, cartesian first quadrant
        if ((tapY - tankY) < 0 && (tapX - tankX) >= 0)
            degToTap += 180;
        //screen third quadrant, cartesian second quadrant
        else if ((tapY - tankY) < 0 && (tapX - tankX) < 0)
            degToTap += 0;
        //screen second quadrant, cartesian third quadrant
        else if ((tapY - tankY) >= 0 && (tapX - tankX) < 0)
            degToTap -= 0;
        //screen first quadrant, cartesian fourth quadrant
        else if ((tapY - tankY) >= 0 && (tapX - tankX) >= 0)
            degToTap -= 180;

        return degToTap;
    }

    public class Tank {

        private ImageView tankIcon;

        public Tank() {
            //find the icon and set it in the center of the frame.
            tankIcon = (ImageView) findViewById(R.id.tank);
            this.setX(frame.getWidth()/2);
            this.setY(frame.getHeight()/2);
        }

        public void setRotation(float degRotation) {
            //point the barrel, not the left side
            tankIcon.setRotation(degRotation - 90);
            //NOTE: the rotation pivot point is in the center (set in activity_main.xml)
        }

        public float getRotation() {
            //return the angle of the barrel, not the left side
            return tankIcon.getRotation() + 90;
        }

        //get and set the center coordinates of the tank
        public void setX(float tankX) {
            tankIcon.setX(tankX - tankIcon.getWidth()/2);
        }
        public float getX() {
            return tankIcon.getX() + tankIcon.getWidth()/2;
        }
        public void setY(float tankY) {
            tankIcon.setY(tankY - tankIcon.getHeight() / 2);
        }
        public float getY() {
            return tankIcon.getY() + tankIcon.getHeight()/2;
        }

        public void shoot() {
            if (shoot) {
                //The bullet obviously has to be visible when it is shot
                bullet.setVisibility(View.VISIBLE);

                //only shoot once.  This is set to true if the bullet hits the tap, or if it exits
                //the screen
                shoot = false;

                //create temporary x, y, and direction variables in the center of the tank
                bulletDirectionDeg = tank.getRotation();
                bulletDirectionRad = bulletDirectionDeg * (float) Math.PI / 180;
                bulletImpactX = tapX;
                bulletImpactY = tapY;

                //cycle through flame icons
                flameIndex++;
                if (flameIndex >= flames.length) flameIndex = 0;
                flames[flameIndex].setX(bulletImpactX - flames[0].getWidth() / 2);
                flames[flameIndex].setY(bulletImpactY);

                //start the bullet with the same coordinates and heading as the tank
                bullet.setX(tank.getX());
                bullet.setY(tank.getY());
                bullet.setRotation(bulletDirectionDeg - 90);

                //update the bullet position every 20 millisecs
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //if the bullet is still in the frame...
                                if (bullet.getX() > 0 && bullet.getX() < frame.getWidth()
                                        && bullet.getY() > 0 && bullet.getY() < frame.getHeight()) {
                                    bullet.setX(bullet.getX() - 5 * (float) Math.cos(bulletDirectionRad));
                                    bullet.setY(bullet.getY() - 5 * (float) Math.sin(bulletDirectionRad));
                                }

                                //when the bullet reaches the tap...
                                if (bullet.getX() < bulletImpactX + 5
                                        && bullet.getX() > bulletImpactX - 5
                                        && bullet.getY() < bulletImpactY + 5
                                        && bullet.getY() > bulletImpactY - 5) {
                                    bullet.setVisibility(View.INVISIBLE);
                                    flames[flameIndex].setVisibility(View.VISIBLE);
                                    shoot = true;
                                }
                            }
                        });
                    }
                }, 0, 20);
            } else {
                bullet.setVisibility(View.INVISIBLE);
                    shoot = (bullet.getX() < 0
                            && bullet.getX() > frame.getWidth()
                            && bullet.getY() < 0
                            && bullet.getY() > frame.getHeight());
            }
        }
    }
}
