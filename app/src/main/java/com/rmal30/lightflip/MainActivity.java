package com.rmal30.lightflip;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    ArrayList<Integer> board;
    int n = 3;
    int s = 2;
    int numMoves;
    ArrayList<String> prevMoves;
    boolean puzzle;
    static SquareAdapter adapter;
    TextView textView;
    Button btnUndo;
    Context ctx = MainActivity.this;
    private GoogleApiClient mGoogleApiClient;
    private boolean signedIn;
    boolean scorePending;
    int savedScore;
    boolean leaderboardPending;
    boolean achievePending;
    boolean useGooglePlay;
    String savedLeaderBoard;
    String savedLeaderBoard2;
    String savedAchievement;
    private class SquareAdapter extends BaseAdapter {
        private ArrayList<Integer> matrix;
        private ArrayList<Integer> old_matrix;
        private int internal_view;
        private int prevPos;
        private boolean ignore;
        private SquareAdapter(Context context, ArrayList<Integer> matrix) {
            this.matrix =  matrix;
            this.internal_view = R.layout.square;
        }
        public void updateData(ArrayList<Integer> matrix){
            ignore = true;
            this.matrix = matrix;
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            Integer value = getItem(position);
            if (convertView == null || convertView.getId() != this.internal_view) {
                convertView = LayoutInflater.from(ctx).inflate(this.internal_view, parent, false);
            }


            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);

            final float scale = getResources().getDisplayMetrics().density;
            imageView.setMinimumHeight((int) Math.floor(scale*(280/n-40/n)));
            imageView.setMinimumWidth((int) Math.floor(scale*(280/n-40/n)));
            int m = (int) Math.floor(20*scale/n);
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
            marginParams.setMargins(m, m, m, m);

            if (old_matrix == null || old_matrix.size()!=matrix.size()) {
                old_matrix = new ArrayList<Integer>();
                for(int i=0; i<matrix.size(); i++){
                    old_matrix.add(0);
                }
            }
            int oldBit = old_matrix.get(position);
            if (!ignore && oldBit != value) {
                Drawable backgrounds[] = new Drawable[4];
                Resources res = getResources();
                if(Build.VERSION.SDK_INT<23){
                    backgrounds[0] = res.getDrawable(R.color.primary);
                    backgrounds[1] = res.getDrawable(R.color.secondary);
                    backgrounds[2] = res.getDrawable(R.color.tertiary);
                    backgrounds[3] = res.getDrawable(R.color.quaternary);

                }else{
                    Resources.Theme theme = ctx.getTheme();
                    backgrounds[0] = res.getDrawable(R.color.primary, theme);
                    backgrounds[1] = res.getDrawable(R.color.secondary, theme);
                    backgrounds[2] = res.getDrawable(R.color.tertiary, theme);
                    backgrounds[3] = res.getDrawable(R.color.quaternary, theme);
                }

                Drawable backgrounds2[] = new Drawable[2];
                backgrounds2[0] = backgrounds[oldBit];
                backgrounds2[1] = backgrounds[value];
                TransitionDrawable crossfader = new TransitionDrawable(backgrounds2);
                imageView.setImageDrawable(crossfader);
                crossfader.startTransition(300);
                old_matrix.set(position, value);
            } else{
                if(Build.VERSION.SDK_INT<23){
                    //noinspection deprecation
                    if (value == 0) {
                        imageView.setBackgroundColor(getResources().getColor(R.color.primary));
                    } else if(value==1){
                        imageView.setBackgroundColor(getResources().getColor(R.color.secondary));
                    } else if(value==2){
                        imageView.setBackgroundColor(getResources().getColor(R.color.tertiary));
                    }else if(value==3){
                        imageView.setBackgroundColor(getResources().getColor(R.color.quaternary));
                    }
                }else{
                    if (value == 0) {
                        imageView.setBackgroundColor(getResources().getColor(R.color.primary, ctx.getTheme()));
                    } else if(value==1){
                        imageView.setBackgroundColor(getResources().getColor(R.color.secondary, ctx.getTheme()));
                    }else if(value==2){
                        imageView.setBackgroundColor(getResources().getColor(R.color.tertiary, ctx.getTheme()));
                    }else if(value==3){
                        imageView.setBackgroundColor(getResources().getColor(R.color.quaternary, ctx.getTheme()));
                    }
                }
            }
            if(ignore){
                ignore = false;
            }
            return convertView;
        }
        @Override
        public int getCount () {
            return n*n;
        }

        @Override
        public Integer getItem (int position){
            return matrix.get(position);
        }

        @Override
        public long getItemId ( int position){
            return position+1;
        }
    }

    public String createNewArray(int n){
        ArrayList<Integer> init = new ArrayList<Integer>();
        int count = 0;
        for(int i=0; i<n*n; i++){
            init.add(0);
        }
        while(count==0){
            int randParity = (int) Math.floor(Math.random()*3);
            for(int j=0; j<n*n+randParity; j++){
                int randPos = (int) Math.floor(Math.random()*n*n);
                for(int k=0; k<n*n; k++){
                    if(randPos%n == k%n || Math.floor(randPos/n)==Math.floor(k/n)){
                        init.set(k, (init.get(k)+1)%s);
                    }
                }
            }
            for(int i=0; i<n*n; i++){
                count+=init.get(i);
            }
        }
        String initStr = "";
        for(int i=0; i<init.size(); i++){
            initStr+=String.valueOf(init.get(i));
        }

        return initStr;
    }

    public void restart(View v){
        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setNumColumns(n);

        final float scale = getResources().getDisplayMetrics().density;
        gridView.setColumnWidth((int) (280/n * scale));
        puzzle = true;
        numMoves = 0;
        prevMoves = new ArrayList<String>();
        String strBoard = createNewArray(n);
        board.clear();
        for(int i=0; i<strBoard.length(); i++){
            board.add((int) strBoard.charAt(i) - (int) '0');
        }
        updateUI(n, numMoves, prevMoves, puzzle);
        adapter.updateData(board);
        adapter.notifyDataSetChanged();
    }


    public void updateUI(int n, int numMoves, ArrayList<String> prevMoves, boolean puzzle){

        String movesStr;
        if(!puzzle){
            movesStr = "Solved in "+numMoves+" moves";
        }else{
            movesStr = "Moves: "+ numMoves;
        }
        textView.setText(movesStr);

        if(prevMoves.size()==0){
            btnUndo.setEnabled(false);
        }else{
            btnUndo.setEnabled(true);
        }

        TextView textView1 = (TextView) findViewById(R.id.rating);
        String rateStr = "";
        switch(n){
            case 2:
            case 3:
                rateStr = "Easy";
                break;
            case 5:
                rateStr = "Medium";
                break;
            case 4:
            case 7:
                rateStr = "Hard";
                break;
            case 6:
                rateStr = "Very Hard";
                break;
            case 8:
                rateStr = "Impossible!";
                break;
        }

        textView1.setText("Rating: "+rateStr+"  -  ");
    }

    public void makeMove(int move){
        for(int i=0; i<n*n; i++) {
            if (move % n == i % n || Math.floor(move / n) == Math.floor(i / n)) {
                board.set(i, (board.get(i)+1)%s);
            }
        }
        updateUI(n, numMoves, prevMoves, puzzle);
        adapter.updateData(board);
        adapter.notifyDataSetChanged();
    }

    public void undoMove(View v){
        try {
            if (prevMoves.size() > 0) {
                if (puzzle) {
                    numMoves--;
                }
                int index = prevMoves.size() - 1;
                for(int i=0; i<s-1; i++){
                    makeMove(Integer.valueOf(prevMoves.get(index)));
                }
                prevMoves.remove(index);
                if (prevMoves.size() == 0) {
                    btnUndo.setEnabled(false);
                }
            }
        }catch(Exception e){
            Toast.makeText(this, "Cannot undo, no more moves left!", Toast.LENGTH_SHORT).show();
            btnUndo.setEnabled(false);
        }
    }

    public void shareApp(MenuItem item){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareSubText = "Light Flip - A color inversion puzzle";
        String shareBodyText = "https://play.google.com/store/apps/details?id=com.rmal30.lightflip&hl=en";
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubText);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBodyText);
        startActivity(Intent.createChooser(shareIntent, "Share With"));
    }

    public void rateApp(MenuItem item){
        try
        {
            Intent rateIntent = rateIntentForUrl("market://details");
            startActivity(rateIntent);
        }
        catch (Exception e)
        {
            Intent rateIntent = rateIntentForUrl("https://play.google.com/store/apps/details");
            startActivity(rateIntent);
        }
    }

    private Intent rateIntentForUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, getPackageName())));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        if (Build.VERSION.SDK_INT >= 21)
        {
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        }
        else
        {
            //noinspection deprecation
            flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
        intent.addFlags(flags);
        return intent;
    }

    boolean isSolved(ArrayList<Integer> board){
        int count = 0;
        for(int i=0; i<board.size(); i++){
            count+=board.get(i);
        }
        return count==0;
    }

    public void showInfo(View v){
        String info = getString(R.string.info);

        new AlertDialog.Builder(this)
                .setTitle("Help").setMessage(info)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){}})
                .setNegativeButton("Hints", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {showHints();}})
                //.setNegativeButton(R.string.rate, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){rateApp();}})
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){shareApp(null);}})
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
    public void showHints(){
        String info = getString(R.string.hint);
        new AlertDialog.Builder(this)
                .setTitle("Hints").setMessage(info)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){}})
                .setNegativeButton(R.string.rate, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){rateApp(null);}})
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which){shareApp(null);}})
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public String getLeaderBoardId(){
        String leaderBoardID = "";
        String prefix = "CgkI-v6Nx_QBEAIQ";
        String suffix ="";
        switch(s*10+n){
            case 24: suffix = "AA"; break;
            case 25: suffix = "Dw"; break;
            case 26: suffix = "Ag"; break;
            case 27: suffix = "Aw"; break;
            case 28: suffix = "Dg"; break;

            case 33: suffix = "Eg"; break;
            case 34: suffix = "FA"; break;
            case 35: suffix = "Fg"; break;
            case 36: suffix = "GA"; break;
            case 37: suffix = "GQ"; break;
            case 38: suffix = "Gg"; break;

            case 42: suffix = "EQ"; break;
            case 43: suffix = "Ew"; break;
            case 44: suffix = "FQ"; break;
            case 45: suffix = "Fw"; break;
            case 46: suffix = "Gw"; break;
            case 47: suffix = "HA"; break;
            case 48: suffix = "HQ"; break;
        }
        if(!suffix.equals("")){
            leaderBoardID = prefix;
        }

        return leaderBoardID+suffix;
    }

    public void showHighScores(View v){
        String leaderBoardID = getLeaderBoardId();
        if(!leaderBoardID.equals("")){
            try{
                mGoogleApiClient.reconnect();
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient, leaderBoardID), 0);
            }catch(Exception e) {
                RC_SIGN_IN = 9001;
                mResolvingConnectionFailure = false;
                mAutoStartSignInFlow = true;
                mSignInClicked = false;
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.clearDefaultAccountAndReconnect();
                } else {
                    mGoogleApiClient.connect();
                }
                leaderboardPending = true;
                savedLeaderBoard2 = leaderBoardID;
            }
        }else{
            Toast.makeText(this, "There is no leaderboard for this level.", Toast.LENGTH_SHORT).show();
        }

    }

    public void showAchievements(View v){
//            try{
        try{
            mGoogleApiClient.reconnect();
            Games.Achievements.unlock(mGoogleApiClient, "CgkI-v6Nx_QBEAIQDA");
            startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient), 0);
        }catch(Exception e){
            RC_SIGN_IN = 9001;
            mResolvingConnectionFailure = false;
            mAutoStartSignInFlow = true;
            mSignInClicked = false;
            if(mGoogleApiClient.isConnected()){
                mGoogleApiClient.clearDefaultAccountAndReconnect();
            }else{
                mGoogleApiClient.connect();
            }
            achievePending = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String strMoves;
        String strBoard;
        n = prefs.getInt("Level", n);
        s = prefs.getInt("Number of colors", s);
        strBoard = prefs.getString("Board string", createNewArray(n));
        strMoves = prefs.getString("History", "");
        numMoves = prefs.getInt("Moves", 0);
        puzzle = prefs.getBoolean("Unsolved", true);
        useGooglePlay = prefs.getBoolean("Use Game API", false);
        prevMoves = new ArrayList<String>(Arrays.asList(strMoves.replaceAll("^\\[|]$", "").replaceAll(" ", "").split(",")));
        board = new ArrayList<Integer>();
        for(int i=0; i<strBoard.length(); i++){
            board.add(Integer.valueOf(Character.toString(strBoard.charAt(i))));
        }
        GridView gridView = (GridView) findViewById(R.id.gridView);
        textView = (TextView) findViewById(R.id.txtNumMoves);
        btnUndo = (Button) findViewById(R.id.btnUndo);
        btnUndo.setEnabled(false);
        adapter = new SquareAdapter(this, board);
        adapter.updateData(board);
        gridView.setNumColumns(n);
        updateUI(n, numMoves, prevMoves, puzzle);
        final float scale = getResources().getDisplayMetrics().density;
        gridView.setColumnWidth((int) (280/n * scale));
        gridView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        if(puzzle){
                            numMoves++;
                        }
                        makeMove(position);
                        prevMoves.add(String.valueOf(position));
                        btnUndo.setEnabled(true);
                        if(isSolved(board) && puzzle){
                            Toast.makeText(ctx, "You solved the puzzle!", Toast.LENGTH_SHORT).show();
                            textView.setText("Solved in "+numMoves+" moves");
                            puzzle = false;

                            if(useGooglePlay) {
                                String leaderBoardID = getLeaderBoardId();

                                String achieveID = "";
                                switch (n) {
                                    case 3:
                                        if(s==2){
                                            achieveID = "CgkI-v6Nx_QBEAIQBA";
                                        }else if(s==3){
                                            achieveID = "CgkI-v6Nx_QBEAIQHw";
                                        }else if(s==4){
                                            achieveID = "CgkI-v6Nx_QBEAIQIA";
                                        }

                                        break;
                                    case 4:
                                        achieveID = "CgkI-v6Nx_QBEAIQBg";
                                        break;
                                    case 5:
                                        achieveID = "CgkI-v6Nx_QBEAIQCw";
                                        break;
                                    case 6:
                                        achieveID = "CgkI-v6Nx_QBEAIQCA";
                                        break;
                                    case 7:
                                        achieveID = "CgkI-v6Nx_QBEAIQBw";
                                        break;
                                    case 8:
                                        achieveID = "CgkI-v6Nx_QBEAIQDQ";
                                        break;
                                }

                                if(s==3 && n>=4){
                                    achieveID = "CgkI-v6Nx_QBEAIQIQ";
                                } else if(s==4 && n>=4){
                                    achieveID = "CgkI-v6Nx_QBEAIQIg";
                                }

                                try {
                                    if (!leaderBoardID.equals("") && numMoves > 4) {
                                        Games.Leaderboards.submitScore(mGoogleApiClient, leaderBoardID, numMoves);
                                    }
                                    if (!achieveID.equals("")) {
                                        Games.Achievements.unlock(mGoogleApiClient, "CgkI-v6Nx_QBEAIQBQ");
                                        Games.Achievements.increment(mGoogleApiClient, achieveID, 1);
                                    }
                                } catch (Exception e) {
                                    RC_SIGN_IN = 9001;
                                    mResolvingConnectionFailure = false;
                                    mAutoStartSignInFlow = true;
                                    mSignInClicked = false;
                                    if (mGoogleApiClient.isConnected()) {
                                        mGoogleApiClient.clearDefaultAccountAndReconnect();
                                    } else {
                                        mGoogleApiClient.connect();
                                    }
                                    scorePending = true;
                                    savedScore = numMoves;
                                    savedLeaderBoard = leaderBoardID;
                                    savedAchievement = achieveID;
                                }
                            }
                        }
                    }
                });


        final SeekBar sk=(SeekBar) findViewById(R.id.seekBar);
        sk.setProgress(n-2);
        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {}
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                n = progress+2;
                restart(null);
            }
        });



        gridView.setAdapter(adapter);
        scorePending = false;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        if(!useGooglePlay){
            LinearLayout gg  = (LinearLayout) findViewById(R.id.google_games);
            gg.setVisibility(View.GONE);
        }
        signedIn = false;
        leaderboardPending = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        signedIn = false;
    }

    protected void onStop(){
        super.onStop();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt("Level", n);
        editor.putInt("Number of colors", s);
        String boardStr = "";
        for(int i=0; i<n*n; i++){
            boardStr+= String.valueOf(board.get(i));
        }
        editor.putString("Board string", boardStr);
        editor.putString("History", Arrays.toString(prevMoves.toArray()));
        editor.putInt("Moves", numMoves);
        editor.putBoolean("Unsolved", puzzle);
        editor.apply();
        if(useGooglePlay){
            mGoogleApiClient.disconnect();
        }

    }
    private static int RC_SIGN_IN = 9001;
    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            return;
        }
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = resolveConnectionFailure(this, mGoogleApiClient, connectionResult, RC_SIGN_IN, "Cannot sign in to Google Play Games.");
        }
    }

    public static boolean resolveConnectionFailure(Activity activity, GoogleApiClient client, ConnectionResult result, int requestCode,
                                                   String fallbackErrorMessage) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(activity, requestCode);
                return true;
            } catch (IntentSender.SendIntentException e) {
                client.connect();
                return false;
            }
        } else {
            // not resolvable... so show an error message
            int errorCode = result.getErrorCode();
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            Dialog dialog = apiAvailability.getErrorDialog(activity, errorCode, requestCode);
            if (dialog != null) {
                dialog.show();
            } else {
                // no built-in dialog: show the fallback error message
                (new AlertDialog.Builder(activity))
                        .setMessage(fallbackErrorMessage)
                        .setNeutralButton(android.R.string.ok, null).create().show();
            }
            return false;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Attempt to reconnect
        mGoogleApiClient.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        MenuItem playItem = menu.getItem(0);
        playItem.setChecked(useGooglePlay);
        return true;
    }

    public void changePlayOption(MenuItem item) {
        //startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        boolean checked = item.isChecked();
        checked = !checked;
        item.setChecked(checked);
        LinearLayout gg  = (LinearLayout) findViewById(R.id.google_games);
        if(checked){
            gg.setVisibility(View.VISIBLE);
        }else{
            gg.setVisibility(View.GONE);
        }
        useGooglePlay = checked;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("Use Game API", checked);
        editor.apply();
    }

public void changeColors(MenuItem menuItem) {

    AlertDialog.Builder b = new AlertDialog.Builder(this);
    b.setTitle("Change color mode:");
    String[] types = {"Standard, 2 colors (Easy)", "Tricolor (Hard)", "Quadcolor (Very hard)"};
    b.setItems(types, new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            s = which + 2;
            restart(null);
            dialog.dismiss();
        }

    });

    b.show();
}
    @Override
    public void onConnected(Bundle connectionHint) {
        signedIn = true;
        try{
            if(scorePending){
                if(!savedLeaderBoard.equals("") && savedScore>4){
                    Games.Leaderboards.submitScore(mGoogleApiClient, savedLeaderBoard,savedScore);
                }
                if(!savedAchievement.equals("")) {
                    Games.Achievements.increment(mGoogleApiClient, savedAchievement, 1);
                }
                Games.Achievements.unlock(mGoogleApiClient, "CgkI-v6Nx_QBEAIQBQ");
                scorePending = false;
            }
            Games.Achievements.unlock(mGoogleApiClient, "CgkI-v6Nx_QBEAIQDA");
            if(leaderboardPending){
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient, savedLeaderBoard2), 0);
                leaderboardPending = false;
            }
            if(achievePending){
                startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient), 0);
                achievePending = false;
            }
        }catch(Exception e){
            Toast.makeText(this, "An error occurred when trying to access leaderboards or achievements, please restart or update the app. If the issue persists, please contact the developer.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
                signedIn = false;
            } else {
                Toast.makeText(this, "Failed to sign in to Google Play Games.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
