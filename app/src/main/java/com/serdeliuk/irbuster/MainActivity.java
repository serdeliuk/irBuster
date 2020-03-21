package com.serdeliuk.irbuster;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    static int n = 0;
    static int b = 0;
    static int cmod = 2;
    public static String cust_res = "";

    public static final int[] WHOLE_CODE = { 0x8, 0x0, 0x5, 0x2, 0x9, 0x0, 0x0, 0x1};
    // 0x80529001 RC6_mode_6A_32 command contain:
    // Vu+ long customer id 8052
    // toggle bit is set (msb "bit 0") inside byte [4] with value 0x9
    // Universal VU RC mode2 set by two bits (2 and 3) in byte[4]
    // KEY_1 code lsb in array, the last one on the right :)
    // it seems that there are left unused 5 bits in [4] which probably can be used for other commands as well entire [5] byte

    public final static int[] KEY_HEADER      = { 2666, 888, 444, 444, 444, 444, 444, 444, 1,444, 444, 888, 888}; // Specific header for RC6 mode 6A
    //                                            +++++-----+++++-----+++++-----+++++-----++-----+++++-----+++++  where + represents ray is on and - ray is off for the usec above
    //                                          <-lead bit--><---RC6-mode-bit------------><---trailer-bit------>
    //                                          Consumer IR ray is formatted from a series of on/off in usec of the ray, ALWAYS firt part emit ray second disable ray, and always only positive values ( i.e. non 0 :) )
    //                                          When it comes to send bits then you need to take care what value had the last part of the last bit sent
    //                                          Above ray represent:
    //                                          2666,888 = Leading bit which emit for 2666usec and then has a pause of 888usec

    //                                          444,444 = first bit from RC6 mode, because the emission before ends with the ray off the first 444 is on and the second is off, which equals with  logical 1 ++++----
    //                                          444,444 = second bit from RC6 mode, because the emission before ends with the ray off the first 444 is on and the second is off, which equals with logical 1 ++++----
    //                                        1,444,444 = third bit from RC6 mode, because we need this bit to be set to zero we add 1 in the front of it to switch on the ray and now equals with logical 0 ----++++
    //                                          RC6 mode 6 ; 110 in binary ; 6 in dec :)

    //                                          888,888 is header's end trailing bit which is twice bigger than normal bit and switch off the ray for 2 x 888 usec

    private static final String TAG = "ConsumerIrBUSTER :)";

    static TextView ray_final;
    static TextView c1;
    static TextView c2;
    static TextView c3;
    static TextView c4;
    static TextView tg;
    static TextView mode;
    static TextView rbit1;
    static TextView rbit2;
    static TextView k1;
    static TextView k2;
    static TextView bust;
    static ConsumerIrManager msCIB;

    // Set the Vu+ mode of the remote control----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // Mode 1 : Solo, Duo
    // Mode 2 : Uno, Ultimo, Solo2, Solo SE, Duo2, Zero, Solo 4K, etc
    // Mode 3 and 4 seems unused, Vu says that are reserved for future use
    static public void SetMode(int m) {
        if ( m == 1) {
            WHOLE_CODE[4] &= ~0b0001; // clear bit
            WHOLE_CODE[4] &= ~0b0010; // clear bit
            mode.setText("1");
        } else if ( m == 2){
            WHOLE_CODE[4] &= ~0b0000; // clear bit
            WHOLE_CODE[4] |=  0b0001; // set bit
            mode.setText("2");
        } else if ( m == 3) {
            WHOLE_CODE[4] |= 0b0010; // set bit
            WHOLE_CODE[4] &= ~0b0001; // clear bit
            mode.setText("3");
        } else if ( m == 4) {
            WHOLE_CODE[4] |= 0b0001; // set bit
            WHOLE_CODE[4] |= 0b0010; // set bit
            mode.setText("4");
        }
        setBuster();
        showRay();
    }

    // increment/decrement multiple int[] from key array at once ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void IncDecKey(final int direction) { // direction 1 = up, 0 = down
        if (direction == 0) {
            if (WHOLE_CODE[7] < 1) {
                WHOLE_CODE[7] = 15;
                k2.setText(String.format("%X", WHOLE_CODE[7]));
                Log.i(TAG, "COMMAND value is " + WHOLE_CODE[7]);
                if (WHOLE_CODE[6] < 1) {
                    WHOLE_CODE[6] = 15;
                    k1.setText(String.format("%X", WHOLE_CODE[6]));
                    Log.i(TAG, "COMMAND value is " + WHOLE_CODE[6]);
                } else {
                    --WHOLE_CODE[7];
                    k2.setText(String.format("%X", WHOLE_CODE[7]));
                    Log.i(TAG, "COMMAND value is " + WHOLE_CODE[7]);
                }
            } else {
                --WHOLE_CODE[7];
                k2.setText(String.format("%X", WHOLE_CODE[7]));
                Log.i(TAG, "COMMAND value is " + WHOLE_CODE[7]);
            }
        } else {
            if (WHOLE_CODE[7] > 14) {
                WHOLE_CODE[7] = 0;
                k2.setText(String.format("%X", WHOLE_CODE[7]));
                if (WHOLE_CODE[6] > 14) {
                    WHOLE_CODE[6] = 0;
                    k1.setText(String.format("%X", WHOLE_CODE[6]));
                } else {
                    ++WHOLE_CODE[6];
                    k1.setText(String.format("%X", WHOLE_CODE[6]));
                }
            } else {
                ++WHOLE_CODE[7];
                k2.setText(String.format("%X", WHOLE_CODE[7]));
            }
        }
        setBuster();
        showRay();
        sendRay();
     }
    // increment/decrement multiple int[] from code array at once ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void IncDecCustomer(final int direction) { // direction 1 = up, 0 = down
        if (direction == 0) {
            if (WHOLE_CODE[3] < 1) {
                WHOLE_CODE[3] = 15;
                c4.setText(String.format("%X", WHOLE_CODE[3]));
                Log.i(TAG, "COMMAND value is " + WHOLE_CODE[3]);
                if (WHOLE_CODE[2] < 1) {
                    WHOLE_CODE[2] = 15;
                    c3.setText(String.format("%X", WHOLE_CODE[2]));
                    Log.i(TAG, "COMMAND value is " + WHOLE_CODE[2]);
                    if (WHOLE_CODE[1] < 1) {
                        WHOLE_CODE[1] = 15;
                        c2.setText(String.format("%X", WHOLE_CODE[1]));
                        Log.i(TAG, "COMMAND value is " + WHOLE_CODE[1]);
                        if (WHOLE_CODE[0] < 1) {
                            WHOLE_CODE[0] = 15;
                            c1.setText(String.format("%X", WHOLE_CODE[0]));
                            Log.i(TAG, "COMMAND value is " + WHOLE_CODE[0]);
                        } else {
                            --WHOLE_CODE[0];
                            c1.setText(String.format("%X", WHOLE_CODE[0]));
                            Log.i(TAG, "COMMAND value is " + WHOLE_CODE[0]);
                        }
                    } else {
                        --WHOLE_CODE[1];
                        c2.setText(String.format("%X", WHOLE_CODE[1]));
                        Log.i(TAG, "COMMAND value is " + WHOLE_CODE[1]);
                    }
                } else {
                    --WHOLE_CODE[2];
                    c3.setText(String.format("%X", WHOLE_CODE[2]));
                    Log.i(TAG, "COMMAND value is " + WHOLE_CODE[2]);
                }
            } else {
                --WHOLE_CODE[3];
                c4.setText(String.format("%X", WHOLE_CODE[3]));
                Log.i(TAG, "COMMAND value is " + WHOLE_CODE[3]);
            }
        } else {
            if (WHOLE_CODE[3] > 14) {
                WHOLE_CODE[3] = 0;
                c4.setText(String.format("%X", WHOLE_CODE[3]));
                if (WHOLE_CODE[2] > 14) {
                    WHOLE_CODE[2] = 0;
                    c3.setText(String.format("%X", WHOLE_CODE[2]));
                    if (WHOLE_CODE[1] > 14) {
                        WHOLE_CODE[1] = 0;
                        c2.setText(String.format("%X", WHOLE_CODE[1]));
                        if (WHOLE_CODE[0] > 14) {
                            WHOLE_CODE[0] = 0;
                            c1.setText(String.format("%X", WHOLE_CODE[0]));
                        } else {
                            ++WHOLE_CODE[0];
                            c1.setText(String.format("%X", WHOLE_CODE[0]));
                        }
                    } else {
                        ++WHOLE_CODE[1];
                        c2.setText(String.format("%X", WHOLE_CODE[1]));
                    }
                } else {
                    ++WHOLE_CODE[2];
                    c3.setText(String.format("%X", WHOLE_CODE[2]));
                }
            } else {
                ++WHOLE_CODE[3];
                c4.setText(String.format("%X", WHOLE_CODE[3]));
            }
        }
        setBuster();
        showRay();
        sendRay();
    }
    // increment/decrement desired byte in consumer data or code data  ------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void IncDecByte(final int codebyte , TextView label, int direction) {
        if ( direction == 0 ) {
            if (WHOLE_CODE[codebyte] < 1) {
                WHOLE_CODE[codebyte] = 15;
                label.setText(String.format("%X", WHOLE_CODE[codebyte]));
            } else {
                --WHOLE_CODE[codebyte];
                label.setText(String.format("%X", WHOLE_CODE[codebyte]));
            }
        }else{
                if (WHOLE_CODE[codebyte] > 14) {
                    WHOLE_CODE[codebyte] = 0;
                    label.setText(String.format("%X", WHOLE_CODE[codebyte]));
                } else {
                    ++WHOLE_CODE[codebyte];
                    label.setText(String.format("%X", WHOLE_CODE[codebyte]));
                }
            }
        setBuster();
        showRay();
    }
    // increment/decrement desired byte in consumer data or code data  ------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void IncDecRbit(final int direction) {
        if (direction == 0) {
            if (WHOLE_CODE[5] < 1) {
                WHOLE_CODE[5] = 15;
                rbit2.setText(String.format("%X", WHOLE_CODE[5]));
                WHOLE_CODE[4] ^= 1 << 2; // toggle bit 3 count start with 0 from lsb
            } else {
                --WHOLE_CODE[5];
                rbit2.setText(String.format("%X", WHOLE_CODE[5]));
            }
        }else{
            if (WHOLE_CODE[5] > 14) {
                WHOLE_CODE[5] = 0;
                rbit2.setText(String.format("%X", WHOLE_CODE[5]));
                WHOLE_CODE[4] ^= 1 << 2; // toggle bit 3 count start with 0 from lsb
            } else {
                ++WHOLE_CODE[5];
                rbit2.setText(String.format("%X", WHOLE_CODE[5]));
            }
        }

        if (WHOLE_CODE[4] << ~2 < 0) { //  check bit 3 is set, count start with 0 from lsb
                rbit1.setText("1");
            } else {
                rbit1.setText("0");
            }
        setBuster();
        showRay();
        sendRay();
         }

    // flip toggle bit as a remote will do ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void SetToggle() {
        WHOLE_CODE[4] ^= 1 << 3;     // toggle bit 3 count start with 0 from lsb
        if (WHOLE_CODE[4] <<~3<0 ) { // check if bit 4 is set, count start with 0 from lsb, better use 0b1000
            tg.setText("1");
        } else {
            tg.setText("0");
        }
        setBuster();
        showRay();
    }
    // update buster button with actual code ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void setBuster(){
        StringBuilder sb = new StringBuilder(WHOLE_CODE.length);
        for(int b: WHOLE_CODE)
            sb.append(String.format("%X", b));
        bust.setText("RC6_6A_32 -- BUST IR -- 0x" + sb);
    }
    // display ray value ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void showRay () {
        int[] res_command = build_command(WHOLE_CODE);
        int[] ray = build_ray(KEY_HEADER, res_command);
        StringBuilder sbh = new StringBuilder(ray.length);
        for(int b: ray)
            sbh.append(b +", ");
        ray_final.setText(sbh);
    }
    // send ray value ocer ir ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public void sendRay() {
        if (!msCIB.hasIrEmitter()) {
            Log.e(TAG, "No IR Emitter found\n");
            ray_final.setText("No IR Emitter found\n");
        } else {
            int[] res_command = build_command(WHOLE_CODE);
            int[] ray = build_ray(KEY_HEADER, res_command);
            msCIB.transmit(36000, ray);
        }
    }
    // build the final int[] to be busted from multiple arrays ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    static public int[] build_ray(final int[] ...arrays ) {
        int size = 0;
        for ( int[] a: arrays )
            size += a.length;

        int[] res = new int[size];

        int destPos = 0;
        for ( int i = 0; i < arrays.length; i++ ) {
            if ( i > 0 ) destPos += arrays[i-1].length;
            int length = arrays[i].length;
            System.arraycopy(arrays[i], 0, res, destPos, length);
        }
        return res;
    }
    // build build ray structure from passed value, here is expected a 32bit value (whole command) -------------------------------------------------------------------------------------------------------------------------------------
    static public int[] build_command(final int[] command ) {
        List<String> list = new ArrayList<>();

        int p = 1;

        for ( int x = 0; x < 8 ; x++) { // iterate through all 8 bytes from command
            n = command[x];

            Log.i("\t\t", "COMMAND value is " + n);
            // First bit (msb) after header came after a high value, in order to be high we should prepend 1, to be low use directly
            // so, first bit (1, 444, 444) is high or (444, 444) is low, if the next one should be high too prepending 1 will switch to low
            //for (int i = 7; i >= 0; i--) {
            for (int i = 3; i >= 0; i--) { // 4 bit testing each turn
                if (n << ~i < 0) { // bit i is set
                    Log.i("\t\t", "testing " + n + " from line " + i + " previous signal was " + p + " the bit is 1");
                    if (p == 1) {
                        list.add("1,444,444");
                        p = 0;
                    } else {
                        list.add("444,444");
                        p = 0;
                    }
                } else { // bit i is NOT set
                    Log.i("\t\t", "testing " + n + " from line " + i + " previous signal was " + p + " the bit is 0");
                    if (p == 1) {
                        list.add("444,444");
                        p = 1;
                    } else {
                        list.add("1,444,444");
                        p = 1;
                    }
                }
            }
        }
        if ( p == 0) {
            list.add("1,9768"); // blast ends with a large space, prepend 1 if the pevious value was low
        } else {
            list.add("9768");
        }

        String s = TextUtils.join(",",list);

        String[] items = s.replaceAll("\\[","").replaceAll("\\]","").replaceAll("\\s","").split(",");

        cust_res = s;
        int[] results = new int[items.length];

        for (int i = 0; i < items.length; i++) {
            try {
                results[i] = Integer.parseInt(items[i]);
            } catch (NumberFormatException nfe) {
                //NOTE: write something here if you need to recover from formatting errors
            };
        }
        return results;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Be sure to call the super class :)
        super.onCreate(savedInstanceState);

        // Get a reference handler to the ConsumerIrManager
        msCIB = (ConsumerIrManager)getSystemService(Context.CONSUMER_IR_SERVICE);

        setContentView(R.layout.activity_main);

        // Set the OnClickListener for the buttons so we see when are pressed.

        findViewById(R.id.cprev_button).setOnClickListener(SetValue);
        findViewById(R.id.cnext_button).setOnClickListener(SetValue);

        findViewById(R.id.next_key).setOnClickListener(SetValue);
        findViewById(R.id.prev_key).setOnClickListener(SetValue);

        findViewById(R.id.bust).setOnClickListener(SetValue);

        findViewById(R.id.c1u).setOnClickListener(SetValue);
        findViewById(R.id.c1d).setOnClickListener(SetValue);
        findViewById(R.id.c2u).setOnClickListener(SetValue);
        findViewById(R.id.c2d).setOnClickListener(SetValue);
        findViewById(R.id.c3u).setOnClickListener(SetValue);
        findViewById(R.id.c3d).setOnClickListener(SetValue);
        findViewById(R.id.c4u).setOnClickListener(SetValue);
        findViewById(R.id.c4d).setOnClickListener(SetValue);

        findViewById(R.id.k1u).setOnClickListener(SetValue);
        findViewById(R.id.k1d).setOnClickListener(SetValue);
        findViewById(R.id.k2u).setOnClickListener(SetValue);
        findViewById(R.id.k2d).setOnClickListener(SetValue);

        findViewById(R.id.tgbtn).setOnClickListener(SetValue);
        findViewById(R.id.modebtn).setOnClickListener(SetValue);

        findViewById(R.id.rbit_next).setOnClickListener(SetValue);
        findViewById(R.id.rbit_prev).setOnClickListener(SetValue);

        ray_final = (TextView) findViewById(R.id.freqs_text);

        c1 = (TextView) findViewById(R.id.c1);
        c2 = (TextView) findViewById(R.id.c2);
        c3 = (TextView) findViewById(R.id.c3);
        c4 = (TextView) findViewById(R.id.c4);

        k1 = (TextView) findViewById(R.id.k1);
        k2 = (TextView) findViewById(R.id.k2);

        bust = (TextView) findViewById(R.id.bust);
        setBuster();
        showRay();

        c1.setText(String.format("%X", WHOLE_CODE[0]));
        c2.setText(String.format("%X", WHOLE_CODE[1]));
        c3.setText(String.format("%X", WHOLE_CODE[2]));
        c4.setText(String.format("%X", WHOLE_CODE[3]));

        k1.setText(String.format("%X", WHOLE_CODE[6]));
        k2.setText(String.format("%X", WHOLE_CODE[7]));


        rbit1 = (TextView) findViewById(R.id.rbit1);
        rbit2 = (TextView) findViewById(R.id.rbit2);

        rbit2.setText(String.format("%X", WHOLE_CODE[5]));

        tg = (TextView) findViewById(R.id.tgval);

        if (WHOLE_CODE[4] <<~3<0 ) { // bit 4 i is set, count start with 0
            tg.setText("1");
        } else {
            tg.setText("0");
        }

        if (WHOLE_CODE[4] <<~2<0 ) { // bit 3 i is set, count start with 0
            rbit1.setText("1");
        } else {
            rbit1.setText("0");
        }

        mode = (TextView) findViewById(R.id.modeval);
        mode.setText("2");
    }

    View.OnClickListener  SetValue = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.c1u: // MSB BYE from customer code UP button
                    IncDecByte(0, c1, 1); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.c1d: // MSB BYE from customer code DOWN button
                    IncDecByte(0, c1, 0); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = down
                    break;

                case R.id.c2u:
                    IncDecByte(1, c2, 1); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.c2d:
                    IncDecByte(1, c2, 0); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.c3u:
                    IncDecByte(2, c3, 1); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.c3d:
                    IncDecByte(2, c3, 0); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.c4u:
                    IncDecByte(3, c4, 1); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.c4d:
                    IncDecByte(3, c4, 0); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.k1u:
                    IncDecByte(6, k1, 1); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.k1d:
                    IncDecByte(6, k1, 0); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.k2u:
                    IncDecByte(7, k2, 1); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.k2d:
                    IncDecByte(7, k2, 0); // address of the byte to change from WHOLE_CODE[]; TextView to update; direction is 1 = up, 0 = dow
                    break;

                case R.id.tgbtn: // toggle button set toggle bit in data code (lsb 4 bytes from whole_code)
                    SetToggle();
                    break;

                case R.id.cnext_button: // increase overall customer code
                    IncDecCustomer(1);
                    break;

                case R.id.cprev_button: // decrease overall customer code
                    IncDecCustomer(0);
                    break;

                case R.id.next_key: // increase overall customer code
                    IncDecKey(1);
                    break;

                case R.id.prev_key: // decrease overall customer code
                    IncDecKey(0);
                    break;

                case R.id.bust: // BUST the desired IR code
                    if (!msCIB.hasIrEmitter()) {
                        Log.e(TAG, "No IR Emitter found\n");
                        ray_final.setText("No IR Emitter found\n");
                    } else {

                        SetToggle();
                        setBuster();
                        showRay();
                        sendRay();
                    }
                    break;

                case R.id.modebtn: // change Vu+ mode to work with other vuplus STB devices, there are only two documented modes and two reserverd, mode 1 and 2 works with  STBs
                    cmod++;
                    if ( cmod > 4 ) {
                        cmod = 1;
                    }
                    SetMode(cmod);
                    break;

                case R.id.rbit_next: // increase last remaining 5 bits into data code in the same byte with toggle and mode
                    IncDecRbit(1);
                    break;

                case R.id.rbit_prev: // decrease last remaining 5 bits into data code in the same byte with toggle and mode
                    IncDecRbit(0);
                    break;

            }
            }
    };
}

